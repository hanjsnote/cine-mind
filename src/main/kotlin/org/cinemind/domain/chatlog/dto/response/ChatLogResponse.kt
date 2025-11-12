package org.cinemind.domain.chatlog.dto.response

import org.cinemind.domain.chatbot.enum.MessageRole
import org.cinemind.domain.chatlog.entity.ChatLog
import java.time.LocalDateTime

/**
 * 대화 기록을 조회할 때 사용하는 response
 */
data class ChatLogResponse (
    // 메시지 전송자 (USER, ASSISTANT)
    val role: MessageRole,

    // 대화 내용
    val content: String,

    // 메시지 생성 시각
    val createdAt: LocalDateTime,

    // RAG 검색에 사용된 키워드
    val queryKeywords: List<String> = emptyList(),

    // 챗봇 답변에 사용된 영화 코드 목록
    val relatedMovieCodes: List<String> = emptyList()
) {
    companion object {
        fun from(chatLog: ChatLog): ChatLogResponse {
            // String으로 저장된 필드들을 조회 시 List<String>으로 변환
            val keywords = chatLog.queryKeywords?.split(", ")?.filter { it.isNotBlank() } ?: emptyList()
            val movieCodes = chatLog.relatedMovieCd?.split(", ")?.filter { it.isNotBlank() } ?: emptyList()

            return ChatLogResponse(
                role = chatLog.role,
                content = chatLog.content,
                createdAt = chatLog.createdAt!!,
                queryKeywords = keywords,
                relatedMovieCodes = movieCodes
            )
        }
    }
}