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
    fun getLogs(userId: Long): List<ChatLogResponse> {
        // DB에서 ChatLog 엔티티 리스트를 조회
        val chatLogs = chatLogRepository.findByUserIdOrderByCreatedAtAsc(userId)

        // 결과를 담을 빈 List 선언
        val responseList = mutableListOf<ChatLogResponse>()

        // for 루프를 돌면서 각 엔티티를 DTO로 반환하여 리스트에 추가
        for (log in chatLogs) {
            val response = ChatLogResponse.from(log)
            responseList.add(response)
        }
        return responseList
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