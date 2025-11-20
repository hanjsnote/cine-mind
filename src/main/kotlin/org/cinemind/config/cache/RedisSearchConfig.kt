package org.cinemind.config.cache

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.StringRedisSerializer
import java.nio.charset.StandardCharsets

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
        try{
            connectionFactory.connection.use { connection ->
                // 1. FT.INFO 명령 실행: 인덱스 존재 여부 확인
                val infoResult = connection.execute("FT.INFO", INDEX_NAME.toByteArray(StandardCharsets.UTF_8))

                // 인덱스가 이미 존재하면 건너뜀.
                if (infoResult != null) {
                    log.info("RedisSearch 인덱스 '{}'가 이미 존재합니다. 생성을 건너뜁니다.", INDEX_NAME)
                    return
                }
            }
        } catch (e: Exception) {
            // 인덱스가 존재하지 않으면 Redis는 예외를 발생.
            log.info("RedisSearch 인덱스 '{}'가 존재하지 않습니다. 생성을 시작합니다.", INDEX_NAME)

            // 2. 인덱스 생성 (FT.CREATE)
            try {
                connectionFactory.connection.use { connection ->

                    // FT.CREATE 명령어 실행
                    val createCommandResult = connection.execute(
                        "FT.CREATE",
                        INDEX_NAME.toByteArray(),
                        "ON".toByteArray(),
                        "JSON".toByteArray(),
                        "PREFIX".toByteArray(),
                        "1".toByteArray(),
                        "cache:".toByteArray(),
                        "SCHEMA".toByteArray(),

                        // TEXT 필드
                        "$.answer".toByteArray(),
                        "AS".toByteArray(),
                        "answer".toByteArray(),
                        "TEXT".toByteArray(),

                        // VECTOR 필드
                        "$.userQuery".toByteArray(),
                        "AS".toByteArray(),
                        "userQuery".toByteArray(),
                        "VECTOR".toByteArray(),
                        "FLAT".toByteArray(),
                        "TYPE".toByteArray(),
                        "FLOAT32".toByteArray(),
                        "DIM".toByteArray(),
                        VECTOR_DIMENSION.toString().toByteArray(),
                        "DISTANCE_METRIC".toByteArray(),
                        "COSINE".toByteArray()
                    )

                    // 보통 성공 시 OK가 Byte Array로 반환되므로, 로그 출력을 위해 String으로 변환.
                    val resultString = if (createCommandResult is ByteArray) {
                        String(createCommandResult, StandardCharsets.UTF_8)
                    } else {
                        createCommandResult.toString()
                    }

                    if (resultString == "OK") {
                        log.info("RedisSearch 인덱스 '{}'가 성공적으로 생성되었습니다. 결과: {}", INDEX_NAME, resultString)
                    } else {
                        log.error("인덱스 '{}' 생성에 실패했습니다. 결과: {}", INDEX_NAME, resultString)
                    }
                }
            } catch (createException: Exception) {
                log.error("RedisSearch 인덱스 생성 중 오류가 발생했습니다: {}. RedisStack 또는 RedisSearch 모듈이 실행중인지 확인해세요.", createException.message)
            }
        }
    }

    // 일반적인 Key-Value 작업을 위한 RedisTemplate 빈 정의.
    @Bean
    open fun redisTemplate(): RedisTemplate<String, Any> {
        val template = RedisTemplate<String, Any>()
        template.setConnectionFactory(connectionFactory)
        // 키는 String 값은 JSON 또는 ByteArray로 처리하여 RedisSearch와 호환되게 설정
        template.keySerializer = StringRedisSerializer()
        // template.valueSerializer = GenericJackson2JsonRedisSerializer() // 일반 데이터용
        return template
    }
}