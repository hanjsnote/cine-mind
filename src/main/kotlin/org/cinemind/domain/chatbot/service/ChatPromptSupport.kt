package org.cinemind.domain.chatbot.service

import org.cinemind.domain.chatbot.enum.MessageRole
import org.cinemind.domain.chatlog.entity.ChatLog
import org.cinemind.domain.rag.dto.model.MovieEmbeddingDto
import org.springframework.stereotype.Component

@Component
class ChatPromptSupport {

    /**
     * LLM에게 전달할 최종 프롬프트 구성
     */
    fun createFullPrompt(
        userQuery: String,
        contextChunks: List<MovieEmbeddingDto>,
        historyList: List<ChatLog>
    ): String {
        val historyString = formatHistory(historyList)

        val contextString = if (contextChunks.isEmpty()) {
            "제공할 Context 정보가 없습니다."
        } else {
            contextChunks.joinToString(separator = "\n---\n") { dto ->
                """
                [검색된 영화 컨텍스트]
                [메타데이터] ${dto.metaText}
                [줄거리 청크] ${dto.plotText}
                """.trimIndent()
            }
        }

        return """
        [CONVERSATION HISTORY]
        $historyString

        [CONTEXT]
        $contextString

        [사용자 질문]
        $userQuery
        """.trimIndent()
    }

    /**
     * 과거 대화를 프롬프트용 문자열로 변환
     */
    fun formatHistory(historyList: List<ChatLog>): String {
        if (historyList.isEmpty()) {
            return "이전 대화 내역이 없습니다."
        }
        return historyList.joinToString(separator = "\n") { log ->
            val role = when (log.role) {
                MessageRole.USER -> "사용자"
                MessageRole.ASSISTANT -> "챗봇"
                else -> log.role.name
            }
            "$role: ${log.content}"
        }
    }

    /**
     * "그 영화", "이 영화" 같은 지시어 질문인지 판별
     */
    fun isDeicticMovieQuestion(query: String): Boolean {
        val normalized = query.replace("\\s+".toRegex(), " ").trim()

        val deicticPatterns = listOf(
            "그 영화", "그 영화의", "그 영화에서",
            "이 영화", "이 영화의", "이 영화에서",
            "그 작품", "이 작품",
            "그 드라마", "이 드라마"
        )
        return deicticPatterns.any { normalized.contains(it) }
    }
}