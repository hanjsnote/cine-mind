package org.cinemind.domain.cache.repository

import com.fasterxml.jackson.databind.ObjectMapper
import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.codec.ByteArrayCodec
import io.lettuce.core.output.NestedMultiOutput
import io.lettuce.core.protocol.CommandArgs
import io.lettuce.core.protocol.ProtocolKeyword
import org.cinemind.domain.cache.dto.model.CacheableChatResponse
import org.cinemind.util.VectorUtils
import org.slf4j.LoggerFactory
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.stereotype.Repository
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * SemanticCacheRepository의 Redis 구현체.
 * 실제 Redis Vector Store와의 통신을 담당.
 */
@Repository
class RedisCacheRepositoryImpl(
    private val connectionFactory: RedisConnectionFactory,
    private val objectMapper: ObjectMapper
) : RedisCacheRepository {

    private val log = LoggerFactory.getLogger(javaClass)
    private val INDEX_NAME = "chat_cache"
    private val KEY_PREFIX = "cache:"
    private val CACHE_TTL_SECONDS = 3600L     // 1시간
    private val MAX_TTL_SECONDS = CACHE_TTL_SECONDS * 2 // 최대 2시간

    private enum class RediSearchCommand(private val keyword: String) : ProtocolKeyword {
        FT_SEARCH("FT.SEARCH");

        override fun getBytes(): ByteArray = keyword.toByteArray()
    }
    /**
     * Redis Vector Store에서 KNN 검색을 수행하고 결과를 반환.
     */
    override fun findClosestVector(
        embedding: FloatArray,
        minSimilarity: Float,
        identifier: String
    ): RedisCacheRepository.CacheHitResult? {

        if (embedding.isEmpty()) return null

        return try {
            val vectorBytes = VectorUtils().floatsToRawByteArray(embedding)

            val lettuceFactory = connectionFactory as? LettuceConnectionFactory
                ?: throw IllegalStateException("RedisConnectionFactory is not LettuceConnectionFactory")

            val conn = lettuceFactory.connection

            @Suppress("UNCHECKED_CAST")
            val async = conn.nativeConnection as? RedisAsyncCommands<ByteArray, ByteArray>
                ?: throw IllegalStateException(
                    "Native connection is not RedisAsyncCommands<ByteArray, ByteArray>: ${conn.nativeConnection::class.qualifiedName}"
                )

            val codec = ByteArrayCodec.INSTANCE
            val args = CommandArgs(codec)
                .add(INDEX_NAME.toByteArray())
                .add("*=>[KNN 1 @userQuery \$vec AS score]".toByteArray())
                .add("PARAMS".toByteArray()).add(2)
                .add("vec".toByteArray()).add(vectorBytes)
                .add("RETURN".toByteArray()).add(2)
                .add("answer".toByteArray())
                .add("score".toByteArray())
                .add("SORTBY".toByteArray()).add("score".toByteArray())
                .add("DIALECT".toByteArray()).add(2)
                .add("LIMIT".toByteArray()).add(0).add(1)

            val output = NestedMultiOutput(codec)

            @Suppress("UNCHECKED_CAST")
            val raw = async.dispatch(
                RediSearchCommand.FT_SEARCH,
                output,
                args
            ).get(5, TimeUnit.SECONDS) as? List<Any?> ?: return null

            log.info("FT.SEARCH raw = {}", raw)
            if (raw.isEmpty()) return null

            //  FT.SEARCH 결과 전체에서 key와 answer/score를 각각 추출
            val redisKey = extractRedisKey(raw)
            val (answer, score) = extractAnswerAndScore(raw) ?: run {
                log.info("FT.SEARCH 결과에서 answer/score 없음 → 캐시 미스 처리. raw={}", raw)
                return null
            }

            val similarity = 1 - score
            if (similarity < minSimilarity) {
                log.info("FT.SEARCH hit 있으나 similarity {} < threshold {}, 캐시 미스 처리", similarity, minSimilarity)
                return null
            }

            // TTL 연장
            if (!redisKey.isNullOrBlank()) {
                val keyBytes = redisKey.toByteArray()
                val currentTtl = conn.keyCommands().ttl(keyBytes) ?: -2L  // -2: 없음, -1: 만료 없음
                val baseTtl = if (currentTtl < 0) 0 else currentTtl
                val newTtl = (baseTtl + CACHE_TTL_SECONDS).coerceAtMost(MAX_TTL_SECONDS)

                if (newTtl > baseTtl) {
                    conn.keyCommands().expire(keyBytes, newTtl)
                    log.info(
                        "Cache HIT key: {} -> TTL {}초로 연장 (기존 {}초)",
                        redisKey, newTtl, currentTtl
                    )
                } else {
                    log.info(
                        "Cache HIT key: {} -> TTL 이미 최대치({}초), 연장 생략 (현재 {}초)",
                        redisKey, MAX_TTL_SECONDS, currentTtl
                    )
                }
            } else {
                log.warn("Cache HIT. 하지만 redisKey를 찾지 못해서 TTL 연장을 건너뜁니다. raw={}", raw)
            }

            // 3) 최종 결과 반환
            RedisCacheRepository.CacheHitResult(
                data = CacheableChatResponse(
                    answer = answer,
                    userQuery = embedding
                ),
                similarity = similarity
            )

        } catch (e: Exception) {
            val root = generateSequence(e as Throwable?) { it.cause }.last()
            log.error(
                "Vector search failed (Lettuce native), top='{}', root='{}'",
                e.message,
                root.message,
                e
            )
            return null
        }
    }

    /**
     * FT.SEARCH의 NestedMultiOutput 결과(raw)를 재귀적으로 순회하면서
     * "answer" / "score" 필드를 찾아내는 헬퍼
     */
    private fun extractAnswerAndScore(node: Any?): Pair<String, Float>? {
        when (node) {
            is List<*> -> {
                if (node.size >= 4) {
                    var answer: String? = null
                    var score: Float? = null

                    var i = 0
                    while (i < node.size - 1) {
                        val name = bytesToString(node[i])
                        val value = node[i + 1]

                        when (name) {
                            "answer" -> answer = bytesToString(value)
                            "score"  -> score  = bytesToString(value).toFloatOrNull()
                        }
                        i += 2
                    }

                    if (answer != null && score != null) {
                        return answer to score
                    }
                }

                // 자식 리스트들 속에 있을 수 있으니 재귀 탐색
                for (child in node) {
                    val found = extractAnswerAndScore(child)
                    if (found != null) return found
                }
            }
        }
        return null
    }

    private fun bytesToString(any: Any?): String =
        when (any) {
            is ByteArray -> any.toString(Charsets.UTF_8)
            null         -> ""
            else         -> any.toString()
        }

    // raw 안을 재귀적으로 돌면서 "cache:"로 시작하는 key를 찾는다
    private fun extractRedisKey(node: Any?): String? {
        when (node) {
            is List<*> -> {
                for (child in node) {
                    val found = extractRedisKey(child)
                    if (found != null) return found
                }
            }
            is ByteArray -> {
                val s = node.toString(Charsets.UTF_8)
                if (s.startsWith(KEY_PREFIX)) return s
            }
            is String -> {
                if (node.startsWith(KEY_PREFIX)) return node
            }
        }
        return null
    }

    /**
     * 질문 벡터와 응답 데이터를 Redis에 저장
     */
    override fun save(embedding: FloatArray, response: CacheableChatResponse) {
        val key = "$KEY_PREFIX${UUID.randomUUID()}"

        try {
            connectionFactory.connection.use { connection ->
                val jsonVector = embedding.joinToString(prefix = "[", postfix = "]")

                val jsonPayload = """
                    {
                      "userQuery": $jsonVector,
                      "answer": "${response.answer}"
                    }
                """.trimIndent()

                connection.commands().execute(
                    "JSON.SET",
                    key.toByteArray(),
                    "$".toByteArray(),
                    jsonPayload.toByteArray()
                )

                connection.keyCommands().expire(key.toByteArray(), CACHE_TTL_SECONDS)
                log.info("Cache 저장 완료 key: {}", key)
            }
        } catch (e: Exception) {
            log.error("Cache 저장 실패: {}", e.message)
        }
    }
}