package org.cinemind.domain.cache.dto.model

import org.cinemind.domain.chatbot.dto.response.ChatLLMResponse

/**
 * Redis L2 Semantic Cache에 저장될 응답 데이터 구조
 *
 * @property answer LLM으로부터 생성된 최종 답변
 * @property userQuery 원본 사용자 질문을 벡터화한 값 (유사도 검색 키로 사용)
 */
data class CacheableChatResponse (
    val answer: String,
    val userQuery: FloatArray
) {
    // FloatArray 배열의 내용(content)을 기반으로 비교할 수 있도록 equals를 수동 재정의
    override fun equals(other: Any?): Boolean {
        if (this === other) return true // 동일 객체 참조 검사
        if (other !is CacheableChatResponse) return false // 타입 검사

        if (answer != other.answer) return false
        // FloatArray의 내용(content)을 비교
        if (!userQuery.contentEquals(other.userQuery)) return false

        return true
    }

    // FloatArray 배열의 내용(content)을 기반으로 해시 코드를 생성할 수 있도록 hashCode를 수동 재정의.
    override fun hashCode(): Int {
        var result = answer.hashCode()
        // FloatArray의 내용을 기반으로 해시 코드 생성
        result = 31 * result + userQuery.contentHashCode()
        return result
    }
}