package org.cinemind.cache.dto.model

import org.cinemind.domain.chatbot.dto.response.ChatLLMResponse

/**
 * Redis L2 Semantic Cache에 저장될 응답 데이터 구조
 *
 * @property answer LLM으로부터 생성된 최종 답변
 * @property queryKeywords 답변을 생성할 때 LLM이 추출한 키워드 (캐시 정규화 목적)
 * @property sources RAG를 통해 사용된 Context의 출처 (메타데이터 및 줄거리 청크)
 * @property embedding 원본 사용자 질문을 벡터화한 값 (유사도 검색 키로 사용)
 */
data class CacheableChatResponse (
    val answer: String,
    val queryKeywords: List<String>,
    val sources: List<String>,
    val embedding: FloatArray
) {
    // FloatArray 배열의 내용(content)을 기반으로 비교할 수 있도록 equals를 수동 재정의
    override fun equals(other: Any?): Boolean {
        if (this === other) return true // 동일 객체 참조 검사
        if (other !is CacheableChatResponse) return false // 타입 검사

        if (answer != other.answer) return false
        if (queryKeywords != other.queryKeywords) return false
        if (sources != other.sources) return false
        // FloatArray의 내용(content)을 비교
        if (!embedding.contentEquals(other.embedding)) return false

        return true
    }

    // FloatArray 배열의 내용(content)을 기반으로 해시 코드를 생성할 수 있도록 hashCode를 수동 재정의.
    override fun hashCode(): Int {
        var result = answer.hashCode()
        result = 31 * result + queryKeywords.hashCode()
        result = 31 * result + sources.hashCode()
        // FloatArray의 내용을 기반으로 해시 코드 생성
        result = 31 * result + embedding.contentHashCode()
        return result
    }
}