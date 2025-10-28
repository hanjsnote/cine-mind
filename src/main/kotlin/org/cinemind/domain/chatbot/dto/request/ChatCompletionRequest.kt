package org.cinemind.domain.chatbot.dto.request

// 챗봇 응답을 받기 위한 DTO
data class ChatCompletionRequest(
    val model: String,
    val messages: List<Message>,
    val temperature: Double = 0.7   // 창의성 조절
)

data class Message(
    val role: String,
    val content: String
)