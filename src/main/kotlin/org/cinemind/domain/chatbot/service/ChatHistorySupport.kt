package org.cinemind.domain.chatbot.service

import org.cinemind.domain.chatbot.enum.MessageRole
import org.cinemind.util.keywordList
import org.cinemind.domain.chatlog.entity.ChatLog
import org.cinemind.domain.chatlog.service.ChatLogService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

/**
 * 로그 저장/조회 + 제목 추출
 */
@Component
class ChatHistorySupport(
    private val chatLogService: ChatLogService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 사용자 메시지를 로그에 저장하고, 최근 대화 내역을 조회해서 반환.
     */
    fun getHistoryAndSaveMessage(
        userId: Long?,
        guestSessionId: String?,
        userQuery: String
    ): Mono<List<ChatLog>> {

        // 1. 사용자 메시지 저장 (Non-Blocking)
        val saveUserMessageMono: Mono<Void> = when {
            userId != null -> {
                Mono.fromRunnable<Void> {
                    log.info("AUTHENTICATED: userId({})가 확인되어 사용자 메시지를 저장합니다.", userId)
                    chatLogService.saveUserMessage(userId, userQuery)
                }.subscribeOn(Schedulers.boundedElastic())
            }

            guestSessionId != null -> {
                Mono.fromRunnable<Void> {
                    log.info("GUEST: sessionId({})가 확인되어 게스트 메시지 저장을 시도합니다.", guestSessionId)
                    chatLogService.saveGuestMessage(guestSessionId, userQuery)
                }.subscribeOn(Schedulers.boundedElastic())
            }

            else -> {
                log.warn("SKIP: userId와 sessionId가 모두 null입니다. 메시지 저장을 건너뜁니다.")
                Mono.empty()
            }
        }

        // 2. 과거 대화 내역 조회
        val historyMono: Mono<List<ChatLog>> = when {
            userId != null -> {
                Mono.fromCallable {
                    chatLogService.getRecentHistory(userId)
                }.subscribeOn(Schedulers.boundedElastic())
            }

            guestSessionId != null -> {
                Mono.fromCallable {
                    chatLogService.getRecentGuestHistory(guestSessionId)
                }.subscribeOn(Schedulers.boundedElastic())
            }

            else -> {
                Mono.just(emptyList())
            }
        }

        return saveUserMessageMono.then(historyMono)
    }

    /**
     * 어시스턴트 응답 로그 저장
     */
    fun saveAssistantMessage(
        userId: Long?,
        guestSessionId: String?,
        answer: String,
        queryKeywords: List<String>,
        relatedMovieCds: List<String>
    ) {
        val saveMono = when {
            userId != null -> {
                Mono.fromRunnable<Void> {
                    chatLogService.chatAssistantMessage(
                        userId = userId,
                        content = answer,
                        queryKeywords = queryKeywords,
                        relatedMovieCds = relatedMovieCds
                    )
                }
            }

            guestSessionId != null -> {
                Mono.fromRunnable<Void> {
                    chatLogService.chatGuestAssistantMessage(
                        sessionId = guestSessionId,
                        content = answer,
                        queryKeywords = queryKeywords,
                        relatedMovieCds = relatedMovieCds
                    )
                }
            }

            else -> Mono.empty()
        }

        saveMono.subscribeOn(Schedulers.boundedElastic()).subscribe()
    }

    /**
     * 최근 대화 내역으로부터 "대표 영화 제목"을 추론. 제목이 아닐 수 있음
     */
    fun findFocusedMovieTitleFromHistory(history: List<ChatLog>): String? {
        // 1) 최근 ASSISTANT 응답들 중에서 queryKeywords 가 채워져 있는 것 찾기
        val lastAssistantWithKeywords = history
            .asReversed()
            .firstOrNull { log ->
                log.role == MessageRole.ASSISTANT &&
                        log.keywordList().isNotEmpty()
            }

        if (lastAssistantWithKeywords != null) {
            return lastAssistantWithKeywords.keywordList().firstOrNull()
        }

        // 2) 그래도 없으면 USER/ASSISTANT 가리지 않고 키워드 있는 마지막 로그 사용
        val lastWithKeywords = history
            .asReversed()
            .firstOrNull { it.keywordList().isNotEmpty() }

        return lastWithKeywords?.keywordList()?.firstOrNull()
    }
}