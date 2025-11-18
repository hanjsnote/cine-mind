package org.cinemind.domain.chatlog.service

import org.springframework.transaction.annotation.Transactional
import org.cinemind.domain.chatlog.dto.response.ChatLogResponse
import org.cinemind.domain.chatlog.entity.ChatLog
import org.cinemind.domain.chatlog.repository.ChatLogRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service

@Service
@Transactional(readOnly = true)
class ChatLogService (
    private val chatLogRepository: ChatLogRepository,
){
    // 대화 메모리로 가져올 최근 메시지 개수
    private val HISTORY_LIMIT = 6

    // ===============================================
    // 1. 인증된 사용자 (Long userId) 관련 로직
    // ===============================================

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

    // ChatService 대화 메모리(Context Window)용 메서드
    // 가장 최근 N개의 대화 기록(Entity)를 반환
    fun getRecentHistory(userId: Long): List<ChatLog> {

        val pageable = PageRequest.of(0, HISTORY_LIMIT, Sort.by(Sort.Direction.DESC, "createdAt"))

        // 최근 6개가 Desc(내림차순)으로 조회
        val recentLogs = chatLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)

        // LLM은 시간 순서(오름차순)로 읽어야 하므로 다시 뒤집어서 반환
        return recentLogs.reversed()
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

    // ===============================================
    // 2. 게스트 세션 (String sessionId) 관련 로직
    // ===============================================

    // 게스트 사용자의 가장 최근 N개의 대화 기록(Entity)를 반환
    fun getRecentGuestHistory(sessionId: String): List<ChatLog> {
        val pageable = PageRequest.of(0, HISTORY_LIMIT, Sort.by(Sort.Direction.DESC, "createdAt"))

        val recentLogs = chatLogRepository.findBySessionIdOrderByCreatedAtDesc(sessionId, pageable)
        return recentLogs.reversed()
    }

    // 게스트(비로그인) 사용자 메시지를 DB에 저장
    @Transactional
    fun saveGuestMessage(sessionId: String, userQuery: String) {
        val guestChatLog = ChatLog.ofGuestMessage(
            sessionId = sessionId,
            content = userQuery
        )
        chatLogRepository.save(guestChatLog)
    }

    // 게스트(비로그인) 챗봇 응답 메시지를 DB에 저장
    @Transactional
    fun chatGuestAssistantMessage(sessionId: String, content: String, queryKeywords: List<String>, relatedMovieCds: List<String>){
        val assistantChatLog = ChatLog.ofGuestResponse(
            sessionId = sessionId,
            content = content,
            queryKeywords = queryKeywords,
            relatedMovieCds = relatedMovieCds
        )
        chatLogRepository.save(assistantChatLog)
    }
}