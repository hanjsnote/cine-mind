package org.cinemind.domain.chatbot.dto.response

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * LLM이 JSON 형식으로 출력할 응답 구조를 정의.
 * LLM 호출 시 답변(answer)과 키워드(queryKeywords)를 한번에 받기 위한 내부 DTO.
 */
data class LlmStructuredResponse (
    // LLM이 생성한 최종 답변 텍스트
    @JsonProperty("answer")
    val answer: String,

    // 사용자 질문에서 추출한 핵심 키워드 목록
    @JsonProperty("queryKeywords")
    val queryKeywords: List<String>
    )