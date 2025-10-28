package org.cinemind.domain.chatbot.dto.request

import org.cinemind.domain.chatbot.dto.message.Message

// 챗봇 응답을 받기 위한 DTO
data class ChatRequest(
    val model: String,
    val messages: List<Message>,
    val temperature: Double = 0.7   // 창의성 조절
)