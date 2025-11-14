package org.cinemind.domain.chatlog.controller

import org.cinemind.common.dto.authuser.AuthUser
import org.cinemind.domain.chatlog.dto.response.ChatLogResponse
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
    @GetMapping
    fun getChatLogs(@AuthenticationPrincipal authUser: AuthUser): List<ChatLogResponse> {
        // authUser.id를 사용하여 해당 사용자의 기록만 서비스 계층에서 조회
        return chatLogService.getLogs(authUser.id)
    }
}