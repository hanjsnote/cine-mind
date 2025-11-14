package org.cinemind.domain.chatbot.dto.response

/**
 * 챗봇의 최종 응답을 사용자에게 전달하기 위한 DTO
 */
class ChatLLMResponse (
    // LLM이 생성한 최종 답변
    val answer: String,

    // 답변의 근거로 사용된 원본 텍스트 청크 목록 (디버깅용)
    val sources: List<String>
)