package org.cinemind.domain.chatlog.controller

import org.cinemind.common.dto.AuthUser
import org.cinemind.domain.chatbot.dto.response.ChatResponse
import org.cinemind.domain.chatlog.service.ChatLogService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/chat-logs")
class ChatLogController (
    private val chatLogService: ChatLogService
) {
    // 챗봇 대화 내역을 저장하는 로직
    @GetMapping
    fun getChatLogs(@AuthenticationPrincipal authUser: AuthUser): List<ChatResponse> {
        // authUser.id를 사용하여 해당 사용자의 기록만 서비스 계층에서 조회
//        return chatLogService.getLogsByUserId(authUser.id)
        return TODO("반환 값을 제공하세요")
    }
}