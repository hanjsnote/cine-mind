package org.cinemind.domain.chatbot.controller

import org.cinemind.domain.chatbot.service.ChatService
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/chat")
class ChatController (
    private val chatService: ChatService
) {

    data class UserQueryRequest(
        val userQuery: String   // 사용자가 입력한 질문
    )

    // 사용자 질문을 처리하고 챗봇 응답을 반환하는 API 엔드포인트
    fun getChatResponse(@RequestBody request: UserQueryRequest): Mono<String> {
        // ChatService에 질문을 위임하여 답변을 받음
        return chatService.getLLMResponse(request.userQuery)
    }
}