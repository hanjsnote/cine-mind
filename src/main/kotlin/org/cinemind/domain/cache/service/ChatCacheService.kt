package org.cinemind.domain.cache.service

import org.cinemind.domain.cache.dto.model.CacheableChatResponse
import org.cinemind.domain.cache.repository.RedisCacheRepository
import org.cinemind.domain.chatbot.dto.response.ChatLLMResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicInteger

/**
 * L2 Semantic Cache의 핵심 로직을 담당하는 서비스
 * 사용자 질문의 벡터화(Embedding)와 Redis 유사도 검색 및 데이터 저장/조회를 처리
 */
@Service
class ChatCacheService (
    private val redisCacheRepository: RedisCacheRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val SIMILARITY_THRESHOLD = 0.6f // 유사도 임계값

    // 누적 카운터
    private val hitCount = AtomicInteger(0)
    private val missCount = AtomicInteger(0)

    // 캐시 조회
    fun retrieveResponse(queryVector: FloatArray, identifier: String): CacheableChatResponse? {
        // Repository를 통해 벡터 유사도 검색 수행
        val result = redisCacheRepository.findClosestVector(
            embedding = queryVector,
            minSimilarity = SIMILARITY_THRESHOLD,
            identifier = identifier
        )

        // 결과 처리
        return if (result != null && result.similarity >= SIMILARITY_THRESHOLD) {

            val hits = hitCount.incrementAndGet()
            val misses = missCount.get()
            val ratio = hits.toDouble() / (hits + misses)

            log.info("Semantic Cache HIT! Similarity={}, Hit={}, Miss={}, HitRatio={}",
                result.similarity,
                hits,
                misses,
                String.format("%.2f", ratio)
            )
            result.data
        } else {
            val misses = missCount.incrementAndGet()
            val hits = hitCount.get()
            val ratio = if (hits + misses == 0) 0.0 else hits.toDouble() / (hits + misses)
            log.info("Semantic Cache MISS.")
            log.info(
                "Semantic Cache MISS. hit={}, miss={}, hitRatio={}",
                hits,
                misses,
                String.format("%.2f", ratio)
            )
            null
        }
    }

    // 캐시 저장
    fun writeResponse(queryVector: FloatArray, chatResponse: ChatLLMResponse) {
        try {
            // CacheableChatResponse DTO 생성
            val cacheData = CacheableChatResponse(
                answer = chatResponse.answer,
                userQuery = queryVector
            )
            // Repository에 저장 요청
            redisCacheRepository.save(queryVector, cacheData)
        } catch (e: Exception) {
            log.error("Failed to write to Semantic Cache", e)
        }
    }
}