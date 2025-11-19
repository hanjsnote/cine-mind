package org.cinemind.cache.service

import org.cinemind.cache.repository.RedisCacheRepository
import org.springframework.stereotype.Service

/**
 * L2 Semantic Cache의 핵심 로직을 담당하는 서비스
 * 사용자 질문의 벡터화(Embedding)와 Redis 유사도 검색 및 데이터 저장/조회를 처리
 */
@Service
class ChatCacheService (
    private val redisCacheRepository: RedisCacheRepository
) {

}