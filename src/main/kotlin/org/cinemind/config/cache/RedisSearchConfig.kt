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
 * Redis 연결 및 Semantic Cache (RedisSearch) 인덱스 설정을 담당하는 Configuration 클래스.
 * RedisSearch 인덱스 정보를 확인하고, 존재하지 않을 경우 FT.CREATE 명령어를 사용하여
 * JSON 기반의 벡터 검색 인덱스 'idx:chat_cache'를 생성.
 */
@Configuration
class RedisSearchConfig (
    private val connectionFactory: RedisConnectionFactory
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // 사용 중인 임베딩 모델의 차원 수 (text-embedding-3-small 기준)
    private val VECTOR_DIMENSION = 1536
    private val INDEX_NAME = "idx:chat_cache"
    private val KEY_PREFIX = "cache:"

    /**
     * 애플리케이션 시작 시 RedisSearch 인덱스를 생성. 인덱스가 이미 존재하면 생성을 건너뛰어 중복 생성을 방지.
     */
    @PostConstruct
    fun createRedisSearchIndex() {
        try{
            connectionFactory.connection.use { connection ->
                // 1. FT.INFO 명령 실행: 인덱스 존재 여부 확인
                val infoResult = connection.execute("FT.INFO", INDEX_NAME.toByteArray(StandardCharsets.UTF_8))

                // FT.INFO가 null이 아닌 결과를 반환하면 인덱스가 존재함을 의미.
                if (infoResult != null) {
                    log.info("RedisSearch 인덱스 '{}'가 이미 존재합니다. 생성을 건너뜁니다.", INDEX_NAME)
                    return
                }
            }
        } catch (e: Exception) {
            // 인덱스가 존재하지 않으면 FT.INFO는 보통 Exception (No such index)을 발생.
            log.info("RedisSearch 인덱스 '{}'가 존재하지 않습니다. 생성을 시작합니다.", INDEX_NAME)

            // 2. 인덱스 생성 (FT.CREATE)
            try {
                connectionFactory.connection.use { connection ->

                    // FT.CREATE 명령어 인자 배열 준비
                    val commandArgs = listOf(
                        INDEX_NAME, "ON", "JSON",
                        "PREFIX", "1", KEY_PREFIX,
                        "SCHEMA",

                        // 1. TEXT 필드 정의: 사용자 응답을 저장할 필드
                        "$.answer", "AS", "answer", "TEXT",

                        // 2. VECTOR 필드 정의: 사용자 쿼리의 임베딩을 저장할 필드
                        "$.userQuery", "AS", "userQuery", "VECTOR",
                        "FLAT", // 인덱스 알고리즘
                        "TYPE", "FLOAT32", // 벡터 데이터 타입
                        "DIM", VECTOR_DIMENSION.toString(), // 벡터 차원
                        "DISTANCE_METRIC", "COSINE" // 유사도 측정 방식
                    ).map { it.toByteArray() }.toTypedArray()

                    // FT.CREATE 명령어 실행
                    val createCommandResult = connection.execute("FT.CREATE", *commandArgs)

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

    /**
     * 일반적인 Key-Value 작업을 위한 RedisTemplate 빈 정의.
     * 키는 String, 값은 Any 타입으로 설정하여 유연하게 사용.
     */
    @Bean
    open fun redisTemplate(): RedisTemplate<String, Any> {
        val template = RedisTemplate<String, Any>()
        template.setConnectionFactory(connectionFactory)
        // 키 시리얼라이저는 String으로 설정
        template.keySerializer = StringRedisSerializer()
        // 값 시리얼라이저는 필요에 따라 GenericJackson2JsonRedisSerializer 등을 사용할 수 있지만, 여기서는 기본 설정을 유지.
        // template.valueSerializer = GenericJackson2JsonRedisSerializer()
        return template
    }
}