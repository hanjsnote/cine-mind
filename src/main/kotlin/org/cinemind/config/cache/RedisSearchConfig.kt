package org.cinemind.config.cache

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory

/**
 * Redis 연결 및 Semantic Cache (RedisSearch) 인덱스 설정을 담당
 */
@Configuration
class RedisSearchConfig (
    private val connectionFactory: RedisConnectionFactory
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val VECTOR_DIMENSION = 1536 // 현재 사용중인 text-embedding-3-small 임베딩 모델의 차원 수
    private val INDEX_NAME = "idx:chat_cache"

    // 인덱스가 존재하지 않으면 애플리케이션 시작 시 RedisSearch 인덱스를 생성
    @PostConstruct
    fun createRedisSearchIndex() {

    }

}