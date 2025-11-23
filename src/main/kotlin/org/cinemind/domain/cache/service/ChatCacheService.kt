package org.cinemind.domain.cache.service

import org.cinemind.domain.cache.dto.model.CacheableChatResponse
import org.cinemind.domain.cache.repository.RedisCacheRepository
import org.cinemind.domain.chatbot.dto.response.ChatLLMResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * L2 Semantic Cache의 핵심 로직을 담당하는 서비스
 * 사용자 질문의 벡터화(Embedding)와 Redis 유사도 검색 및 데이터 저장/조회를 처리
 */
@Service
class ChatCacheService (
    private val redisCacheRepository: RedisCacheRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val SIMILARITY_THRESHOLD = 0.75f // 유사도 임계값

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
            log.info("Semantic Cache HIT! Similarity: {}", result.similarity)
            result.data
        } else {
            log.info("Semantic Cache MISS.")
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