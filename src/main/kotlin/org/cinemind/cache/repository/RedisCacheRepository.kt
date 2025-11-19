package org.cinemind.cache.repository

import org.cinemind.cache.dto.model.CacheableChatResponse

/**
 * Semantic Cache (Redis Vector Store)와의 상호작용을 담당하는 Repository.
 * 벡터 유사도 검색 및 Key-Value 저장을 처리.
 */
interface RedisCacheRepository {
    /**
     * 벡터 유사도 검색을 수행하여 가장 유사한 캐시 데이터를 찾는다.
     * @param embedding 검색에 사용할 질문 벡터
     * @param minSimilarity 최소 유사도 임계값
     * @param identifier 사용자 ID 또는 세션 ID (캐시 분리용)
     * @return CacheHitResult(data: CacheableChatResponse, similarity: Float) 또는 null
     */
    fun findClosestVector(
        embedding: FloatArray,
        minSimilarity: Float,
        identifier: String,
    ): CacheHitResult?

    /**
     * 질문 벡터를 키로 사용하여 응답 데이터를 캐시에 저장.
     * @param embedding 저장할 질문 벡터
     * @param response 저장할 응답 데이터 DTO
     */
    fun save(embedding: FloatArray, response: CacheableChatResponse)
    // 유사도 검색 결과를 담는 내부 데이터 클래스
    data class CacheHitResult(
        val data: CacheableChatResponse,
        val similarity: Float
    )
}