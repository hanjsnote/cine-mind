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
    @Column(name = "user_id", nullable = true)
    val userId: Long? = null,

    // 게스트 세션 ID (비로그인 사용자용)
    @Column(name = "session_id", nullable = true)
    val sessionId: String? = null,

    // MessageRole Enum 사용 (USER, ASSISTANT)
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
    val relatedMovieCd: String? = null

) : BaseEntity() {

    init {
        // userId와 sessionId 중 하나는 반드시 존재해야 함을 보장
        require(userId != null || sessionId != null) {"userId와 sessionId는 동시에 null이 될 수 없습니다."}
    }

    companion object {
        private const val MAX_CONTENT_LENGTH = 1000 // 대화 내용 최대 길이
        // 사용자(USER) 메시지 인스턴스 생성 (인증된 사용자 - Long ID)
        fun ofUserMessage(userId: Long, content: String): ChatLog {

            val safeContent = content.take(MAX_CONTENT_LENGTH)

            return ChatLog(
                userId = userId,
                role = MessageRole.USER,
                content = safeContent,
                queryKeywords = null,
                relatedMovieCd = null
            )
        }

        // 챗봇(CHATBOT) 응답 메시지 인스턴스 생성 (인증된 사용자 - Long ID)
        fun ofChatbotResponse(
            userId: Long,
            content: String,
            queryKeywords: List<String>,
            relatedMovieCds: List<String>
        ): ChatLog {
            val safeContent = content.take(MAX_CONTENT_LENGTH)

              return ChatLog(
                  userId = userId,
                  role = MessageRole.ASSISTANT,
                  content = safeContent,
                  queryKeywords = queryKeywords.joinToString(", "), // List -> String 변환 저장
                  relatedMovieCd = relatedMovieCds.joinToString(", ") // List -> String 변환 저장
              )
        }

        // 게스트 메시지 인스턴스 생성 (비로그인 사용자 - String Session ID)
        fun ofGuestMessage(sessionId: String, content: String): ChatLog {
            val safeContent = content.take(MAX_CONTENT_LENGTH)
            return ChatLog(
                sessionId = sessionId,
                role = MessageRole.USER,
                content = safeContent,
                queryKeywords = null,
                relatedMovieCd = null
            )
        }

        // 게스트 챗봇 응답 메시지 인스턴스 생성 (비로그인 사용자 - String Session ID)
        fun ofGuestResponse(
            sessionId: String,
            content: String,
            queryKeywords: List<String>,
            relatedMovieCds: List<String>
        ): ChatLog {
            val safeContent = content.take(MAX_CONTENT_LENGTH)

            return ChatLog(
                sessionId = sessionId,
                role = MessageRole.ASSISTANT,
                content = safeContent,
                queryKeywords = queryKeywords.joinToString(", "),
                relatedMovieCd = relatedMovieCds.joinToString(", ")
            )
        }
    }
}