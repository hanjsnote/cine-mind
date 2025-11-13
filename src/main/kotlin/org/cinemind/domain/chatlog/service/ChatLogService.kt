package org.cinemind.domain.chatlog.service

import org.cinemind.common.exception.CommonErrorCode
import org.cinemind.common.exception.GlobalException
import org.springframework.transaction.annotation.Transactional
import org.cinemind.domain.chatbot.dto.response.ChatResponse
import org.cinemind.domain.chatbot.enum.MessageRole
import org.cinemind.domain.chatlog.dto.response.ChatLogResponse
import org.cinemind.domain.chatlog.entity.ChatLog
import org.cinemind.domain.chatlog.repository.ChatLogRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class ChatLogService (
    private val chatLogRepository: ChatLogRepository,
){
    // 특정 사용자 ID의 모든 대화 내역을 시간 순으로 조회
    fun getLogsByUserId(userId: Long): List<ChatLogResponse> {

        return TODO("반환 값을 제공하세요")
    }

    // 사용자(USER) 메시지를 DB에 저장
    @Transactional
    fun saveUserMessage(userId: Long, userQuery: String) {
        val userChatLog = ChatLog.ofUserMessage(
            userId = userId,
            content = userQuery
        )
        chatLogRepository.save(userChatLog)
    }

    // 챗봇 ASSISTANT 메시지와 관련 메타 데이터를 DB에 저장
    @Transactional
    fun chatAssistantMessage(userId: Long, content: String, queryKeywords: List<String>, relatedMovieCds: List<String>) {
        val assistantChatLog = ChatLog.ofChatbotResponse(
            userId = userId,
            content = content,
            queryKeywords = queryKeywords,
            relatedMovieCds = relatedMovieCds
        )
        chatLogRepository.save(assistantChatLog)
    }
}