package org.cinemind.config.cache

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.data.redis.connection.RedisConnectionFactory

/**
 * RedisSearch 인덱스를 생성하는 로직
 * 존재 여부를 미리 확인하지 않고 먼저 FT.CREATE를 시도
 * 새로 만들어지면 성공, 이미 있을 경우 'Index already exists' 로그 출력
 */
@Configuration
@Profile("!test")
class RedisSearchConfig(
    private val connectionFactory: RedisConnectionFactory
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val VECTOR_DIMENSION = 1536
    private val INDEX_NAME = "chat_cache"
    private val KEY_PREFIX = "cache:"

    @PostConstruct
    fun createRedisSearchIndex() {
        connectionFactory.connection.use { conn ->
            try {
                val args = listOf(
                    INDEX_NAME, "ON", "JSON",
                    "PREFIX", "1", KEY_PREFIX,
                    "SCHEMA",
                    "$.userQuery", "AS", "userQuery",
                    "VECTOR", "HNSW", "6",
                    "TYPE", "FLOAT32",
                    "DIM", VECTOR_DIMENSION.toString(),
                    "DISTANCE_METRIC", "COSINE",
                    "$.answer", "AS", "answer", "TEXT"
                ).map { it.toByteArray() }.toTypedArray()

                val res = conn.execute("FT.CREATE", *args)
                log.info("RedisSearch FT.CREATE 성공. 결과: {}", res?.toString() ?: "null")

            } catch (e: Exception) {
                // 루트 cause까지 타고 내려가서 실제 Redis 에러 메시지 확인
                val root = generateSequence(e as Throwable?) { it.cause }.last()
                val rootMsg = root.message ?: ""
                val topMsg = e.message ?: ""

                if (rootMsg.contains("Index already exists", ignoreCase = true) ||
                    rootMsg.contains("Index name is already in use", ignoreCase = true)
                ) {
                    log.info(
                        "RedisSearch 인덱스 '{}'는 이미 존재합니다. (root msg: '{}') 생성은 건너뜁니다.",
                        INDEX_NAME,
                        rootMsg
                    )
                } else {
                    log.error(
                        "RedisSearch 인덱스 '{}' 생성 중 알 수 없는 오류 발생. top='{}', root='{}'",
                        INDEX_NAME,
                        topMsg,
                        rootMsg,
                        e
                    )
                }
            }
        }
    }
}