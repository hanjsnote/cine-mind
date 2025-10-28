package org.cinemind.domain.chatbot.dto.message

import org.cinemind.domain.chatbot.enum.MessageRole

data class Message(
    val role: MessageRole,
    val content: String
)