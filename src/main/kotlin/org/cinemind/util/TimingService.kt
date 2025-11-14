package org.cinemind.util

import org.cinemind.config.jwt.JwtAuthenticationFilter
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

/**
 * Reactor Mono의 실행 시간을 측정하고 로그를 출력하는 서비스
 */
@Service
class TimingService {

    companion object {
        private val log = LoggerFactory.getLogger(TimingService::class.java)
    }
    // source Mono<T> 시간을 측정할 반응형 스트림
    // operationName String 로그에 출력할 작업 이름 (예: "LLM API 호출")
    fun <T> measure(source: Mono<T>, operationName: String): Mono<T> {
        // elapsed()를 사용하여 시간(t1)과 데이터(t2)를 포함하는 Tuple2로 반환
        return source.elapsed()
            .doOnNext { tuple ->
                val elapsedTimeMs = tuple.t1
                val resultData = tuple.t2

                log.info("[Timing] '$operationName' 작업 시간 측정 결과")
                log.info(" -> 소요 시간: ${elapsedTimeMs}ms")
            }
            // Tuple에서 실제 데이터만 추출하여 스트림으로 복구
            .map {it.t2}
    }
}