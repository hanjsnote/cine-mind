package org.cinemind.domain.chatlog.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.cinemind.common.entity.BaseEntity
import org.cinemind.domain.chatbot.enum.MessageRole

/**
 * 챗봇과 대화 내역을 저장/조회하는 엔티티
 */
@Entity
@Table(name="chat_logs")
class ChatLog (

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    // 사용자 ID (AuthUser 에서 직접 받음)
    @Column(name = "user_id", nullable = false)
    val userId: Long,

    // MessageRole Enum 사용 (USER, SYSTEM)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val role: MessageRole,

    // 대화 내용
    @Column(nullable = false, columnDefinition = "TEXT")
    val content: String,

    // RAG 검색에 사용된 키워드
    @Column(name = "query_keywords", columnDefinition = "TEXT")
    val queryKeywords: String? = null,

    // RAG 검색 결과로 사용된 영화 코드 목록
    @Column(name = "related_movie_codes", columnDefinition = "TEXT")
    val relatedMovieCodes: String? = null

) : BaseEntity() {
    companion object {
        private const val MAX_CONTENT_LENGTH = 2000 // 대화 내용 최대 길이
        // 사용자(USER)가 보낸 메시지 인스턴스를 생성
        fun ofUserMessage(userId: Long, content: String): ChatLog {

            val safeContent = content.take(MAX_CONTENT_LENGTH)

            return ChatLog(
                userId = userId,
                role = MessageRole.USER,
                content = safeContent,
                queryKeywords = null,
                relatedMovieCodes = null
            )
        }
        // 챗봇(CHATBOT)의 응답 메시지 인스턴스 생성
        fun ofChatbotResponse(
            userId: Long,
            content: String,
            queryKeywords: List<String>,
            relatedMovieCodes: List<String>
        ): ChatLog {
            val safeContent = content.take(MAX_CONTENT_LENGTH)

              return ChatLog(
                  userId = userId,
                  role = MessageRole.ASSISTANT,
                  content = safeContent,
                  queryKeywords = queryKeywords.joinToString(", "), // List -> String 변환 저장
                  relatedMovieCodes = relatedMovieCodes.joinToString(", ") // List -> String 변환 저장
              )
        }
    }
}