package org.cinemind.cache.repository

import com.fasterxml.jackson.databind.ObjectMapper
import org.cinemind.cache.dto.model.CacheableChatResponse
import org.cinemind.util.VectorUtils
import org.slf4j.LoggerFactory
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.stereotype.Repository
import java.nio.charset.StandardCharsets
import java.util.UUID

/**
 * SemanticCacheRepository의 Redis 구현체.
 * 실제 Redis Vector Store와의 통신을 담당.
 */
@Repository
class RedisCacheRepositoryImpl (
    private val connectionFactory: RedisConnectionFactory,
    private val objectMapper: ObjectMapper
) : RedisCacheRepository {

    private val log = LoggerFactory.getLogger(javaClass)
    private val INDEX_NAME = "idx:chat_cache"
    private val KEY_PREFIX = "cache:"
    private val CACHE_TTL_SECONDS = 3600L // 1시간

    // Redis Vector Store에서 KNN 검색을 수행하고 결과를 반환.
    override fun findClosestVector(
        embedding: FloatArray,
        minSimilarity: Float,
        identifier: String
    ): RedisCacheRepository.CacheHitResult? {
        // 검색용 Raw Binary Vector 생성
        val vectorBytes = VectorUtils().floatsToRawByteArray(embedding)

        // 명령어 실행을 위한 인자 구성
        val searchArgs = listOf(
            INDEX_NAME.toByteArray(),
            "*=>[KNN 1 @userQuery \$vec AS score]".toByteArray(),
            "PARAMS".toByteArray(),
            "2".toByteArray(),
            "vec".toByteArray(),
            vectorBytes,
            "RETURN".toByteArray(),
            "3".toByteArray(),
            "$.answer".toByteArray(),
            "AS".toByteArray(),
            "answer".toByteArray(),
            "score".toByteArray(),
            "SORTBY".toByteArray(),
            "score".toByteArray(),
            "DIALECT".toByteArray(),
            "2".toByteArray(),
            "LIMIT".toByteArray(),
            "0".toByteArray(),
            "1".toByteArray()
        ).toTypedArray()
        return try {
            connectionFactory.connection.use { connection ->
                // FT.SEARCH 실행
                val resultList = connection.commands().execute("FT.SEARCH", *searchArgs) as? List<*>
                    ?: return null

                // 결과 파싱
                if (resultList.isEmpty() || resultList[0] as Long == 0L)
                    return null // 검색 결과 없음

                val fields = resultList[2] as List<*>
                var answer: String? = null
                var score: Float? = null

                for (i in fields.indices step 2) {
                    val name = fields[i] as String
                    val value = fields[i + 1]

                    when (name) {
                        "answer" -> answer = value.toString()
                        "score" -> score = value.toString().toFloat()
                    }
                }

                if (answer == null || score == null) return null

                // score가 작을수록 유사 — cosine 기준
                val similarity = 1 - score
                if (similarity < minSimilarity) return null

                RedisCacheRepository.CacheHitResult(
                    data = CacheableChatResponse(
                        answer = answer,
                        userQuery = embedding
                    ),
                    similarity = similarity
                )
            }
        } catch (e: Exception) {
            log.error("Vector search failed: {}", e.message)
            null
        }
    }

    // 질문 벡터와 응답 데이터를 Redis에 저장
    override fun save(embedding: FloatArray, response: CacheableChatResponse) {
        val key = "$KEY_PREFIX${UUID.randomUUID()}"

        try {
            connectionFactory.connection.use { connection ->
            // 1. DTO를 JSON 문자열로 직렬화
            val jsonString = objectMapper.writeValueAsString(response)

            // 2. JSON.SET 명령어 실행
                connection.commands().execute(
                    "JSON.SET",
                    key.toByteArray(),
                    "$".toByteArray(),
                    jsonString.toByteArray()
                )
                connection.keyCommands().expire(key.toByteArray(), CACHE_TTL_SECONDS)
            }

            log.info("Saved cache: {}", key)
        } catch (e: Exception) {
            log.error("Failed to save cache: {}", e.message)
        }
    }
}