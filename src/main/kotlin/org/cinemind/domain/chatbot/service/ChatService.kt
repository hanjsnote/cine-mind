package org.cinemind.domain.chatbot.service

import org.cinemind.cache.dto.model.CacheableChatResponse
import org.cinemind.cache.service.ChatCacheService
import org.cinemind.common.dto.authuser.AuthUser
import org.cinemind.domain.chatbot.client.OpenAiClient
import org.cinemind.domain.chatbot.dto.response.ChatLLMResponse
import org.cinemind.domain.chatbot.dto.response.LlmStructuredResponse
import org.cinemind.domain.chatlog.entity.ChatLog
import org.cinemind.domain.chatlog.service.ChatLogService
import org.cinemind.domain.rag.client.RagEmbeddingClient
import org.cinemind.domain.rag.dto.model.MovieEmbeddingDto
import org.cinemind.domain.rag.service.RagRetrievalService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

/**
 * 챗봇의 핵심 비즈니스 로직(RAG 오케스트레이션)을 담당하는 서비스
 * L2 Semantic Cache와 RAG 파이프라인을 통합하여 최적화된 응답 흐름을 처리합니다.
 *
 * 1. Vectorize (벡터화)
 * 2. L2 Cache Lookup (벡터 검색)
 * 3. Retrieval (검색)
 * 4. Augmentation (증강)
 * 5. Generation (생성)
 * 6. Cache Write (캐시 저장)
 * 7. Memory Write (로그 저장)
 * */
@Service
class ChatService (
    private val openAiClient: OpenAiClient,
    private val ragRetrievalService: RagRetrievalService,
    private val chatLogService: ChatLogService,
    private val chatCacheService: ChatCacheService,
    private val ragEmbeddingClient: RagEmbeddingClient, // 벡터화를 위해 RagEmbeddingClient 주입
){
    private val log = LoggerFactory.getLogger(javaClass)

    // 챗봇 페르소나 및 답변 규칙 정의 (이전 내용과 동일)
    private val SYSTEM_INSTRUCTION = """
        당신은 '시네마인드'의 전문 영화 추천 및 정보 제공 챗봇입니다.
        사용자의 질문에 대해 항상 친절하고 정확하게 답변해야 합니다.
        // ... (이하 동일)
        
        **[키워드 추출 규칙 (캐시 정규화 목적)]**
        1. 추출 키워드는 질문의 **가장 핵심적인 4개의 고유 명사나 주제어**만 포함합니다. (예: 인물, 영화 제목, 특정 사건, 핵심 개념)
        2. '영화', '드라마', '내용', '정보', '추천', '궁금', '어떻게', '알려줘' 등 **질문 형식이나 일반적인 장르 명사는 일절 제외**합니다.
        3. 추출된 키워드는 캐시 키 정규화를 위해 **반드시 알파벳 또는 가나다순으로 정렬**하여 배열에 담아야 합니다.
        {
          "answer": "문맥과 사용자 질문에 기반한 최종 답변 텍스트",
          "queryKeywords": ["핵심 키워드 1 (정렬됨)", "핵심 키워드 2 (정렬됨)", "핵심 키워드 3 (정렬됨)"]
        }    
        
        1. 답변시에는 제공된 [CONTEXT] 정보를 최우선으로 활용해야 합니다.
        2. 만약 [CONTEXT]가 "제공할 Context 정보가 없습니다."로 비어 있다면, **당신의 기본 지식이나 일반 상식을 일절 활용하지 말고**, 오직 다음 문구만 출력해야 합니다: "제가 가진 영화 정보에는 해당 내용이 없습니다." 이외의 **어떠한 부가 설명도 절대 금지**합니다.
        3. [CONTEXT] 정보가 있다면, 답변은 사용자가 영화에 흥미를 느낄 수 있도록 매력적이고 간결하게 작성해주세요.
        4. 답변 형식은 항상 한국어로 작성해야 합니다.
    """.trimIndent()

    // RAG Context 확보, 최종 프롬프트 생성, LLM 호출을 통합
    fun getLLMResponse(authUser: AuthUser?, sessionId: String?, userQuery: String): Mono<ChatLLMResponse> {
        val userId = authUser?.id
        val guestSessionId = sessionId
        val identifier = userId?.toString() ?: guestSessionId ?: "anonymous" // 캐시 식별자

        val startTime = System.currentTimeMillis()

        // 1. 사용자 질문을 벡터화 L2 캐시 조회와 RAG 검색에 모두 사용
        val queryVectorMono: Mono<FloatArray> = Mono.fromCallable {
            ragEmbeddingClient.getEmbedding(userQuery)
        }.subscribeOn(Schedulers.boundedElastic())
            .cache() // 벡터화는 한 번만 수행하도록 캐싱

        // 2. 생성된 벡터로 Semantic Cache 조회
        val cacheResponseMono: Mono<CacheableChatResponse?> = queryVectorMono
            .flatMap { vector ->
                if (vector.isNotEmpty()) {
                    Mono.fromCallable {
                        chatCacheService.retrieveResponse(vector, identifier)
                    }.subscribeOn(Schedulers.boundedElastic())
                } else {
                    Mono.justOrEmpty(null)
                }
            }
            .cache() // 캐시 조회 결과도 캐싱

        // 3. 캐시 히트 여부에 따른 분기 처리
        val finalResponseMono = cacheResponseMono.flatMap { cachedResponse ->
            if (cachedResponse != null) {
                // ** [Cache HIT] **
                log.info("L2 Semantic Cache HIT. 응답을 즉시 반환합니다.")
                // 캐시 히트 시, 응답 객체를 Mono로 변환
                Mono.just(ChatLLMResponse(
                    answer = cachedResponse.answer,
                    // 캐시 응답은 출처(sources) 정보가 없으므로 빈 리스트 반환
                    sources = emptyList()
                ))
            } else {
                // ** [Cache MISS] - Full RAG Pipeline 실행 **
                log.info("L2 Semantic Cache MISS. Full RAG 파이프라인을 실행합니다.")

                // 4-1. 대화 메모리 조회 + 사용자 메시지 저장
                val historyAndSaveMono = getHistoryAndSaveMessage(userId, guestSessionId, userQuery)

                // 4-2. (RAG) Retrieval, Augmentation, Generation 통합 실행
                Mono.zip(historyAndSaveMono, queryVectorMono)
                    .flatMap { tuple ->
                        val historyList = tuple.t1 // 튜플의 첫 번째 요소: List<ChatLog>
                        val vector = tuple.t2    // 튜플의 두 번째 요소: FloatArray (재사용)

                        // 4-3. Context 확보: 이미 생성된 벡터 사용
                        val contextMono = Mono.fromCallable {
                            ragRetrievalService.retrieveRelevantContext(vector)
                        }.subscribeOn(Schedulers.boundedElastic())

                        contextMono.flatMap { contextChunks ->
                            // 4-4. 최종 프롬프트 생성
                            val fullPrompt = createFullPrompt(userQuery, contextChunks, historyList)

                            // 4-5. LLM 호출
                            openAiClient.getChatCompletion(
                                SYSTEM_INSTRUCTION,
                                fullPrompt,
                                LlmStructuredResponse::class.java
                            ).map { structuredResponse ->
                                // LLM 응답 후, Triple로 묶어 다음 단계로 전달
                                val sources = contextChunks.map {
                                    "[메타데이터] ${it.metaText}\n[줄거리] ${it.plotText}"
                                }
                                val chatResponse = ChatLLMResponse(
                                    answer = structuredResponse.answer,
                                    sources = sources
                                )
                                Triple(chatResponse, structuredResponse.queryKeywords, vector) // 벡터도 함께 전달
                            }
                        }
                    }
                    .doOnSuccess { (chatResponse, queryKeywords, queryVector) ->
                        // 5. 응답을 Semantic Cache에 저장 (비동기 Fire-and-Forget)
                        Mono.fromRunnable<Void> {
                            chatCacheService.writeResponse(queryVector, chatResponse)
                        }.subscribeOn(Schedulers.boundedElastic()).subscribe()

                        // 6. 챗봇 응답 저장 (비동기 Fire-and-Forget)
                        val relatedMovieCds = chatResponse.sources.mapNotNull { extractMovieCd(it) }.distinct()
                        saveAssistantMessage(userId, guestSessionId, chatResponse.answer, queryKeywords, relatedMovieCds)
                    }
                    .map { (chatResponse, _, _) -> chatResponse } // 최종 ChatLLMResponse 반환
            }
        }

        return finalResponseMono
            .doOnTerminate {
                val elapsed = System.currentTimeMillis() - startTime
                log.info("사용자 질문 요청 - 최종 응답 객체 생성까지 걸리는 시간 : {}ms", elapsed)
            }
    }

    // 사용자 메시지 저장과 대화 내역 조회를 통합하는 Mono
    private fun getHistoryAndSaveMessage(userId: Long?, guestSessionId: String?, userQuery: String): Mono<List<ChatLog>> {
        // 1. 사용자 메시지 저장 (Non-Blocking, Fire-and-Forget)
        val saveUserMessageMono: Mono<Void> = if (userId != null) {
            Mono.fromRunnable<Void> {
                log.info("AUTHENTICATED: userId({})가 확인되어 사용자 메시지를 저장합니다.", userId)
                chatLogService.saveUserMessage(userId, userQuery)
            }
        } else if (guestSessionId != null) {
            Mono.fromRunnable<Void> {
                log.info("GUEST: sessionId({})가 확인되어 게스트 메시지 저장을 시도합니다.", guestSessionId)
                chatLogService.saveGuestMessage(guestSessionId, userQuery)
            }
        } else {
            log.warn("SKIP: userId와 sessionId가 모두 null입니다. 메시지 저장을 건너뜁니다.")
            Mono.empty()
        }.subscribeOn(Schedulers.boundedElastic())

        // 2. 과거 대화 내역 조회
        val historyMono: Mono<List<ChatLog>> = if (userId != null) {
            Mono.fromCallable {
                chatLogService.getRecentHistory(userId)
            }
        } else if (guestSessionId != null) {
            Mono.fromCallable {
                chatLogService.getRecentGuestHistory(guestSessionId)
            }
        } else {
            Mono.just(emptyList())
        }.subscribeOn(Schedulers.boundedElastic())

        // 저장 완료 후, 기록 조회를 반환
        return saveUserMessageMono.then(historyMono)
    }

    // 어시스턴트 메시지 저장을 위한 Non-Blocking 처리
    private fun saveAssistantMessage(
        userId: Long?,
        guestSessionId: String?,
        answer: String,
        queryKeywords: List<String>,
        relatedMovieCds: List<String>
    ) {
        val saveMono = if (userId != null) {
            Mono.fromRunnable<Void> {
                chatLogService.chatAssistantMessage(
                    userId = userId,
                    content = answer,
                    queryKeywords = queryKeywords,
                    relatedMovieCds = relatedMovieCds
                )
            }
        } else if (guestSessionId != null) {
            Mono.fromRunnable<Void> {
                chatLogService.chatGuestAssistantMessage(
                    sessionId = guestSessionId,
                    content = answer,
                    queryKeywords = queryKeywords,
                    relatedMovieCds = relatedMovieCds
                )
            }
        } else {
            Mono.empty()
        }

        saveMono.subscribeOn(Schedulers.boundedElastic()).subscribe()
    }

    // 소스에서 movieCd를 추출하는 유틸리티 함수 (ChatLog 저장을 위해)
    private fun extractMovieCd(source: String): String? {
        val regex = Regex("\\[메타데이터] (.*?) \\[줄거리]")
        // 메타데이터에서 movieCd를 추출하는 로직 구현 필요
        // 예: 메타데이터 문자열에서 "movieCd: 20201234" 패턴을 찾아서 추출
        val cdRegex = Regex("movieCd: (\\d+)")
        return cdRegex.find(source)?.groups?.get(1)?.value
    }


    /**
     * LLM에게 전달할 최종 프롬프트를 생성.
     * [CONVERSATION HISTORY] + [CONTEXT] + [사용자 질문] 구조
     */
    private fun createFullPrompt(
        userQuery: String,
        contextChunks: List<MovieEmbeddingDto>,
        historyList: List<ChatLog>
    ): String{
        // 1. 대화 내역 포맷팅
        val historyString = formatHistory(historyList)

        // 2. RAG Context 포맷팅
        val contextString = if (contextChunks.isEmpty()) {
            "제공할 Context 정보가 없습니다."
        } else {
            contextChunks.joinToString (separator = "\n---\n"){ dto ->
                """
              [검색된 영화 컨텍스트]
              [메타데이터] ${dto.metaText}
              [줄거리 청크] ${dto.plotText}
              """.trimIndent()
            }
        }

        // 3. 최종 프롬프트 구성
        return """
        [CONVERSATION HISTORY]
        $historyString

        [CONTEXT]
        $contextString

        [사용자 질문]
        $userQuery
        """.trimIndent()
    }

    // 과거 대화 내역 리스트(List<ChatLog>)를 LLM 프롬프트에 넣기 좋은 형식으로 변환.
    private fun formatHistory(historyList: List<ChatLog>): String {
        if (historyList.isEmpty()) {
            return "이전 대화 내역이 없습니다."
        }
        // ChatLog 엔티티의 'role'과 'content' 필드 사용
        return historyList.joinToString(separator = "\n") { log ->
            val role = when (log.role) {
                org.cinemind.domain.chatbot.enum.MessageRole.USER -> "사용자"
                org.cinemind.domain.chatbot.enum.MessageRole.ASSISTANT -> "챗봇"
                else -> log.role.name // SYSTEM 등
            }
            "$role: ${log.content}"
        }
    }
}