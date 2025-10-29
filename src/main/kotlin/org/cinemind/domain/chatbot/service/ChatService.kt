package org.cinemind.domain.chatbot.service

import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class ChatService {
    fun getLLMResponse(userQuery: String): Mono<String> {
        return TODO("반환 값을 제공하세요")
    }
}