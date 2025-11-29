package org.cinemind.domain.chatbot.service

import org.cinemind.common.dto.authuser.AuthUser
import org.cinemind.domain.cache.dto.model.CacheableChatResponse
import org.cinemind.domain.cache.service.ChatCacheService
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
        
        출력 형식에 대한 절대 규칙
        1. 당신의 출력은 **반드시 JSON 객체 하나**여야 합니다.
        2. JSON 객체의 필드는 **오직 두 개**만 허용됩니다.
           - "answer": 문자열
           - "queryKeywords": 문자열 배열
        3. JSON 바깥에 어떤 내용도 출력하면 안 됩니다.
           - 인사, 사과, 설명, 주석, 마크다운, 코드블럭, 공백 줄 등 **어떠한 추가 텍스트도 금지**합니다.
        4. 실제 응답은 아래 예시 형식과 100% 동일한 구조여야 합니다.
        
        예시 (설명용):
        {
          "answer": "사용자 질문에 대한 최종 한국어 답변 문장",
          "queryKeywords": ["키워드1", "키워드2", "키워드3"]
        }
        
        아래와 같은 출력은 절대 금지입니다.
        - "제가 추천하는 영화는..." 처럼 JSON 없이 문장만 출력
        - ```json 으로 시작하거나 ```로 끝나는 마크다운 코드블럭
        - JSON 뒤에 설명 문장을 추가하는 경우
        
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

        /**
         * 1) 사용자 질문 벡터화
         */
        val queryVectorMono: Mono<FloatArray> = Mono.fromCallable {
            ragEmbeddingClient.getEmbedding(userQuery)
        }.subscribeOn(Schedulers.boundedElastic())
            .cache() // 벡터화는 한 번만 수행하도록 캐싱

        /**
         * 2) 생성된 벡터로 Semantic Cache 조회
         */
        val cacheResponseMono: Mono<CacheableChatResponse> = queryVectorMono
            .flatMap { vector ->
                if (vector.isNotEmpty()) {
                    Mono.defer {
                        val cached = chatCacheService.retrieveResponse(vector, identifier)
                        Mono.justOrEmpty(cached)
                    }.subscribeOn(Schedulers.boundedElastic())
                } else {
                    Mono.empty()
                }
            }
            .doOnError { e ->
                log.error("!!! ERROR: [Cache Lookup] 캐시 조회 중 오류 발생: {}", e.message)
            }
            .cache() // 캐시 조회 결과도 캐싱

        /**
         * 3) 캐시 HIT → 바로 응답
         * 4) 캐시 MISS → RAG 전체 파이프라인 실행
         */
        // 캐시 / RAG 공통으로 쓸 user 메시지 저장 + 대화 내역 조회
        val historyMono: Mono<List<ChatLog>> = getHistoryAndSaveMessage(userId, guestSessionId, userQuery)
            .doOnError { e -> log.error("!!! ERROR: 대화 내역 조회/저장 중 오류 발생: {}\", e.message", e.message) }
            .onErrorReturn(emptyList())
            .cache()

        val finalResponseMono: Mono<ChatLLMResponse> = cacheResponseMono
            // ** [Cache HIT] **
            .flatMap { cachedResponse ->
                historyMono.then(   // user 메시지 저장 완료까지 기다렸다가
                    Mono.fromCallable {
                        // assistant 로그도 남겨 둔다 (키워드는 일단 비워도 됨)
                        saveAssistantMessage(
                            userId,
                            guestSessionId,
                            cachedResponse.answer,
                            emptyList(),
                            emptyList()
                        )
                        ChatLLMResponse(
                            answer = cachedResponse.answer,
                            sources = emptyList()
                        )
                    }.subscribeOn(Schedulers.boundedElastic())
                )
            }

            // ** [Cache MISS] - Full RAG Pipeline 실행 **
            .switchIfEmpty(
                Mono.defer {
                    log.info("L2 Semantic Cache MISS. Full RAG 파이프라인을 실행합니다.")

                    // 4-1. (RAG) Retrieval, Augmentation, Generation 통합 실행
                    Mono.zip(historyMono, queryVectorMono)
                        .flatMap { tuple ->
                            val historyList = tuple.t1
                            val vector = tuple.t2

                            // 지시어("그 영화 ~") 질문인지 먼저 판별
                            val isDeictic = isDeicticMovieQuestion(userQuery)

                            // 콜드 스타트 + 지시어인 경우: 바로 안내 메시지 리턴
                            val hasAnyKeywords = historyList.any { it.keywordList().isNotEmpty() }
                            if (isDeictic && !hasAnyKeywords) {
                                log.info("지시어 질문이지만 대화 내역이 없는 콜드 스타트입니다. RAG를 실행하지 않고 안내 메시지를 반환합니다. query={}", userQuery)

                                val response = ChatLLMResponse(
                                    answer = "어떤 영화를 말씀하시는지 아직 알 수 없어요. 영화 제목을 알려주시면 줄거리를 찾아드릴게요.",
                                    sources = emptyList()
                                )

                                // 캐시는 안 써도 되니까 키워드 리스트는 비워서 넘겨 줌
                                return@flatMap Mono.just(
                                    Triple(response, emptyList<String>(), vector)
                                )
                            }

                            // 4-2. Context 확보
                            val contextMono = Mono.fromCallable {
                                if (isDeictic) {
                                    // 대화 내역에서 가장 최근에 다룬 영화 제목 찾기
                                    val focusedTitle = findFocusedMovieTitleFromHistory(historyList)

                                    if (focusedTitle != null) {
                                        log.info("지시어 질문으로 판단되어, 최근 영화 '{}' 제목으로 RAG 검색을 수행합니다.", focusedTitle)

                                        // 영화 제목을 벡터화해서 검색
                                        val titleVector = ragEmbeddingClient.getEmbedding(focusedTitle)
                                        ragRetrievalService.retrieveRelevantContext(titleVector)
                                    } else {
                                        log.info("지시어 질문이지만 대화 내역에서 영화 제목을 찾지 못했습니다. 원래 쿼리 벡터로 검색합니다.")
                                        ragRetrievalService.retrieveRelevantContext(vector)
                                    }
                                } else {
                                    // 일반 질문은 기존처럼 현재 질문 벡터로 검색
                                    ragRetrievalService.retrieveRelevantContext(vector)
                                }
                            }.subscribeOn(Schedulers.boundedElastic())

                            contextMono.flatMap { contextChunks ->
                                // 4-3. 최종 프롬프트 생성
                                val fullPrompt = createFullPrompt(userQuery, contextChunks, historyList)

                                // 4-4. LLM 호출
                                openAiClient.getChatCompletion(
                                    SYSTEM_INSTRUCTION,
                                    fullPrompt,
                                    LlmStructuredResponse::class.java
                                ).map { structuredResponse ->
                                    val sources = contextChunks.map {
                                        // LLM 응답 후, Triple로 묶어 다음 단계로 전달
                                        "[메타데이터] ${it.metaText}\n[줄거리] ${it.plotText}"
                                    }
                                    val chatResponse = ChatLLMResponse(
                                        answer = structuredResponse.answer,
                                        sources = sources
                                    )
                                    Triple(chatResponse, structuredResponse.queryKeywords, vector)
                                }
                            }
                        }
                        .doOnSuccess { (chatResponse, queryKeywords, queryVector) ->

                            val ambiguous = isDeicticMovieQuestion(userQuery)

                            /**
                             * 5) 성공적으로 응답 생성 → 모호하지 않은 쿼리만 글로벌 캐시 저장(예: '그 영화 줄거리가 뭐지?'는 캐시 저장 안함)
                             */
                            if (!ambiguous) {
                                Mono.fromRunnable<Void> {
                                    chatCacheService.writeResponse(queryVector, chatResponse)
                                }.subscribeOn(Schedulers.boundedElastic()).subscribe()
                            } else {
                                log.info("모호한 질문은 캐시 저장을 건너뜁니다. query='{}'", userQuery)
                            }

                            /**
                             * 6) 챗봇 응답 저장
                             */
                            val relatedMovieCds = chatResponse.sources.mapNotNull { extractMovieCd(it) }.distinct()
                            saveAssistantMessage(userId, guestSessionId, chatResponse.answer, queryKeywords, relatedMovieCds)
                        }
                        .map { (chatResponse, _, _) -> chatResponse }
                        .switchIfEmpty(
                            Mono.just(
                                ChatLLMResponse(
                                    "LLM 생성 실패로 인해 답변을 생성하지 못했습니다. 로그를 확인해주세요.",
                                    emptyList()
                                )
                            )
                        )
                }
            )

        return finalResponseMono
            .doOnTerminate {
                val elapsed = System.currentTimeMillis() - startTime
                log.info("사용자 질문 요청 - 최종 응답 객체 생성까지 걸리는 시간 : {}ms", elapsed)
            }
    }

    /**
     * 대화 기록 저장 + 조회
     */
    private fun getHistoryAndSaveMessage(userId: Long?, guestSessionId: String?, userQuery: String): Mono<List<ChatLog>> {
        // 1. 사용자 메시지 저장 (Non-Blocking, Fire-and-Forget)
        val saveUserMessageMono: Mono<Void> = if (userId != null) {
            Mono.fromRunnable<Void> {
                log.info("AUTHENTICATED: userId({})가 확인되어 사용자 메시지를 저장합니다.", userId)
                chatLogService.saveUserMessage(userId, userQuery)
            }.subscribeOn(Schedulers.boundedElastic())

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
            }.subscribeOn(Schedulers.boundedElastic())

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

    /**
     * 어시스턴트 메시지 저장
     */
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

    /**
     * 소스에서 movieCd를 추출하는 유틸리티 함수 (ChatLog 저장을 위해)
     */
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

    // 글로벌 캐시 부분: "대화 맥락에 의존하는 모호한 영화 질문"인지 판별
    private fun isDeicticMovieQuestion(query: String): Boolean {
        val normalized = query.replace("\\s+".toRegex(), " ").trim()

        // 1) "그/이 영화" 같은 전형적인 지시 표현이 들어 있는지
        val deicticPatterns = listOf(
            "그 영화", "그 영화의", "그 영화에서",
            "이 영화", "이 영화의", "이 영화에서",
            "그 작품", "이 작품",
            "그 드라마", "이 드라마"
        )
        val hasDeictic = deicticPatterns.any { normalized.contains(it) }

        return hasDeictic
    }

    private fun findFocusedMovieTitleFromHistory(history: List<ChatLog>): String? {
        // 1) 최근 ASSISTANT 응답들 중에서 queryKeywords 가 채워져 있는 것 찾기
        val lastAssistantWithKeywords = history
            .asReversed() // 최신 로그부터 거꾸로
            .firstOrNull { log ->
                log.role == org.cinemind.domain.chatbot.enum.MessageRole.ASSISTANT &&
                        log.keywordList().isNotEmpty()
            }

        if (lastAssistantWithKeywords != null) {
            // 첫 번째 키워드를 "대표 영화"로 사용 (예: "모아나 2")
            return lastAssistantWithKeywords.keywordList().firstOrNull()
        }

        // 2) 그래도 없으면 USER/ASSISTANT 가리지 않고 키워드 있는 마지막 로그 사용
        val lastWithKeywords = history
            .asReversed()
            .firstOrNull { it.keywordList().isNotEmpty() }

        return lastWithKeywords?.keywordList()?.firstOrNull()
    }

    private fun ChatLog.keywordList(): List<String> {
        return this.queryKeywords
            ?.split(",")                // "모아나 2, 마우이, 애니메이션"
            ?.map { it.trim() }         // 공백 제거
            ?.filter { it.isNotBlank() } // 빈 문자열 제거
            ?: emptyList()
    }
}