package org.cinemind.domain.chatbot.client

import org.apache.catalina.Role
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class OpenAiClient(
    private val webClient: WebClient,
    @Value("\${openal.api.key")
    private val openApiKey: String
) {
    // LLM 모델 정보 정의
    private val LLM_MODEL = "gpt-4o-mini"
    private val API_URI = "https://api.openai.com/v1/chat/completions"








}