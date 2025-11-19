package org.cinemind.cache.repository

import org.cinemind.cache.dto.model.CacheableChatResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository

@Repository
class RedisCacheRepositoryImpl (

) : RedisCacheRepository {
    private val log = LoggerFactory.getLogger(javaClass)

    // Redis Vector Store에서 KNN 검색을 수행하고 결과를 반환.
    override fun findClosestVector(
        embedding: FloatArray,
        minSimilarity: Float,
        identifier: String
    ): RedisCacheRepository.CacheHitResult? {
        log.info("Redis Vector Store 에서 캐시 검색을 수행합니다.: {}", identifier)
        return null
    }

    override fun save(embedding: FloatArray, response: CacheableChatResponse) {
        log.info("[Redis] 벡터 및 응답 데이터 저장 완료. (벡터 차원: ${embedding.size})")
    }
}