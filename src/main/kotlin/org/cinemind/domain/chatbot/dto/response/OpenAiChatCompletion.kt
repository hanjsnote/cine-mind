package org.cinemind.domain.chatbot.dto.response

import org.cinemind.domain.chatbot.dto.message.Message

/**
 * OpenAI API 응답을 받기 위한 DTO
 */
data class OpenAiChatCompletion(
    val id: String,
    val choices: List<Choice>
)

data class Choice(
    val message: Message
)