package org.cinemind.domain.chatbot.enum

import com.fasterxml.jackson.annotation.JsonValue

enum class MessageRole (
    @JsonValue
    val roleValue: String
){
    SYSTEM("system"),
    USER("user"),
    ASSISTANT("assistant")
}