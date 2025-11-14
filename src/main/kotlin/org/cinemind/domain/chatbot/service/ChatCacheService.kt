package org.cinemind.domain.chatbot.service

import org.cinemind.domain.chatbot.dto.response.ChatLLMResponse
import reactor.core.publisher.Mono

/**
 * 챗봇 응답 캐싱을 담당하는 서비스 인터페이스
 * 캐시 키는 키워드 목록을 기반으로 생성
 */
interface ChatCacheService {
    // 캐시 저장소에서 키워드에 해당하는 응답을 조회
    // return 캐시된 ChatResponse (발견되면 Mono.just(), 없으면 Mono.empty())
    fun findCachedResponse(queryKeywords: List<String>): Mono<ChatLLMResponse>

    // 새로운 응답을 캐시 저장소에 저장
    fun saveCache(queryKeywords: List<String>, chatLLMResponse: ChatLLMResponse): Mono<Boolean>
}