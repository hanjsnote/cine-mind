package org.cinemind.config.openai

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

// application.yml에서 openai 관련 설정을 바인딩하기 위한 클래스
@ConfigurationProperties("openai.api")
data class OpenAiConfigProperties(
    val key: String,
    val embeddingUrl: String,
    val chatUrl:String
)

