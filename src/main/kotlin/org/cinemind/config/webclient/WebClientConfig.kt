package org.cinemind.config.webclient

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

// 외부 API 통신을 위한 WebClient 설정 파일
@Configuration
class WebClientConfig {

    @Bean
    fun webClient(builder: WebClient.Builder): WebClient {
        // 기본 WebClient 빌더를 사용하여 WebClient 빈을 생성
        return builder.build()
    }
}