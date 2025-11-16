package org.cinemind.cache.dto.model

import org.cinemind.domain.chatbot.dto.response.ChatLLMResponse

/**
 * Redis에 저장될 캐시 엔트리 DTO
 * L0/L1 캐시는 광범위한 질문과 구체적인 질문 두 가지를 모두 저장한다.
 */
data class ChatCacheEntry (
    // 캐시 타입: FULL_ANSWER(최종 답변)
    val type: CacheEntryType,

    // type == FULL_ANSWER 일 때 저장되는 값
    val fullResponse: ChatLLMResponse? = null,

    // type == CONTEXT_IDS 일 때 저장되는 값 (관련 영화 CD 리스트)
    val movieCds: List<String>? = null
)
enum class CacheEntryType {
    FULL_ANSWER,
    CONTEXT_IDS
}