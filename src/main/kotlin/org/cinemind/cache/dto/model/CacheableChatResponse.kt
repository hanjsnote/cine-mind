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
    val embedding: List<Float>
)