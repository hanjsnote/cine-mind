package org.cinemind.domain.chatbot.service

import org.cinemind.common.dto.authuser.AuthUser
import org.cinemind.domain.chatbot.client.OpenAiClient
import org.cinemind.domain.chatbot.dto.response.ChatLLMResponse
import org.cinemind.domain.chatbot.dto.response.LlmStructuredResponse
import org.cinemind.domain.chatlog.entity.ChatLog
import org.cinemind.domain.chatlog.service.ChatLogService
import org.cinemind.domain.rag.dto.model.MovieEmbeddingDto
import org.cinemind.domain.rag.service.RagRetrievalService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers // **[수정 1] Schedulers import 추가**

/**
 * 챗봇의 핵심 비즈니스 로직(RAG 오케스트레이션)을 담당하는 서비스
 * 이 서비스는 다음 단계를 통해 실행: (Memory -> R -> A -> G)
 * 1. Memory (메모리): ChatLogService를 통해 과거 대화 내역 확보 (대화 연속성)
 * 2. Retrieval (검색): RagRetrievalService를 통해 Context 확보 (사실적 근거)
 * 3. Augmentation (증강): 확보된 Memory와 Context로 프롬프트 증강
 * 4. Generation (생성): OpenAiClient를 통해 답변 생성
 * */
@Service
class ChatService (
    private val openAiClient: OpenAiClient,
    private val ragRetrievalService: RagRetrievalService,
    private val chatLogService: ChatLogService
//    private val chatCacheService: ChatCacheService    // 캐싱은 나중에 추가
){
    private val log = LoggerFactory.getLogger(javaClass)

    // 챗봇 페르소나 및 답변 규칙 정의 (이전 내용과 동일)
    private val SYSTEM_INSTRUCTION = """
        당신은 '시네마인드'의 전문 영화 추천 및 정보 제공 챗봇입니다.
        사용자의 질문에 대해 항상 친절하고 정확하게 답변해야 합니다.
        
        **[새로운 규칙: 대화 연속성]**
        1. 제공된 [CONVERSATION HISTORY]를 활용하여 **문맥과 대화 연속성**을 유지하고 답변해야 합니다.
        2. 현재 질문이 이전 대화의 연장선상에 있다면, 이전 대화의 정보를 활용하여 현재 질문을 해석하고 답변하세요.
        
        **[출력 형식 규칙]**
        **당신은 어떠한 추가 설명 없이 오직 JSON 형식의 객체 하나만 출력해야 합니다.**
        JSON은 반드시 다음 스키마를 따라야 하며 'queryKeywords' 필드에는 캐시 키 생성을 위해 다음과 같은 핵심 키워드를 추출해야 합니다.
        
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

        // 요청 시작 시간 측정
        val startTime = System.currentTimeMillis()

        // 1. (사용자 메시지 저장 - Non-Blocking)
        // Blocking I/O를 Mono.fromRunnable로 감싸고 boundedElastic 스케줄러에서 실행하도록 위임
        val saveUserMessageMono: Mono<Void> = if (userId != null) {
            Mono.fromRunnable<Void> {
                log.info("AUTHENTICATED: userId({})가 확인되어 사용자 메시지를 저장합니다.", userId)
                chatLogService.saveUserMessage(userId, userQuery)
            }.subscribeOn(Schedulers.boundedElastic())
        } else if (guestSessionId != null) {
            Mono.fromRunnable<Void> {
                log.info("GUEST: sessionId({})가 확인되어 게스트 메시지 저장을 시도합니다.", guestSessionId)
                chatLogService.saveGuestMessage(guestSessionId, userQuery)
            }.subscribeOn(Schedulers.boundedElastic())
        } else {
            log.warn("SKIP: userId와 sessionId가 모두 null입니다. 메시지 저장을 건너뜁니다.")
            Mono.empty()
        }

        // 2. (대화 메모리 - Non-Blocking)** 과거 대화 내역 조회
        val historyMono: Mono<List<ChatLog>> = if (userId != null) {
            Mono.fromCallable {
                chatLogService.getRecentHistory(userId)
            }.subscribeOn(Schedulers.boundedElastic()) // Blocking I/O 스케줄러 지정
        } else if (guestSessionId != null) {
            Mono.fromCallable {
                chatLogService.getRecentGuestHistory(guestSessionId)
            }.subscribeOn(Schedulers.boundedElastic()) // Blocking I/O 스케줄러 지정
        } else {
            Mono.just(emptyList())
        }

        // 3. (RAG 1단계 - 검색 - Non-Blocking)** Context 확보
        val contextMono = Mono.fromCallable { ragRetrievalService.retrieveRelevantContext(userQuery) }
            .subscribeOn(Schedulers.boundedElastic()) // Blocking I/O 스케줄러 지정

        // 4. 사용자 메시지 저장 완료 후, 기록 조회와 Context 검색을 병렬로 시작 (Mono.zip)
        return saveUserMessageMono.then(Mono.zip(historyMono, contextMono))
            .flatMap { tuple ->
                val historyList = tuple.t1 // 튜플의 첫 번째 요소: List<ChatLog>
                val contextChunks = tuple.t2 // 튜플의 두 번째 요소: List<MovieEmbeddingDto>

                // 5. (Augmentation) 대화 내역과 Context를 기반으로 최종 프롬프트 생성
                val fullPrompt = createFullPrompt(userQuery, contextChunks, historyList)

                // 6. (Generation) LLM 호출 (OpenAiClient는 논블로킹 WebClient 기반이라고 가정)
                val structuredResponseMono = openAiClient.getChatCompletion(
                    SYSTEM_INSTRUCTION,
                    fullPrompt,
                    LlmStructuredResponse::class.java
                )

                // LLM 응답 Mono와 Context 청크를 묶어서 다음 단계로 전달
                structuredResponseMono.map { it to contextChunks }
            }
            .map { (structuredResponse, contextChunks) ->
                val sources = contextChunks.map {
                    "[메타데이터] ${it.metaText}\n[줄거리] ${it.plotText}"
                }
                val chatResponse = ChatLLMResponse(
                    answer = structuredResponse.answer,
                    sources = sources
                )
                // Triple로 묶어 다음 체인에 전달 (ChatLLMResponse, 키워드, 청크)
                Triple(chatResponse, structuredResponse.queryKeywords, contextChunks)
            }
            .doOnSuccess { (chatResponse, queryKeywords, contextChunks) ->
                // 7. 챗봇 응답 저장 (Non-Blocking, Fire-and-Forget)
                val relatedMovieCds = contextChunks.map { it.movieCd }

                val saveAssistantMessageMono = if (userId != null) {
                    Mono.fromRunnable<Void> {
                        chatLogService.chatAssistantMessage(
                            userId = userId,
                            content = chatResponse.answer,
                            queryKeywords = queryKeywords,
                            relatedMovieCds = relatedMovieCds
                        )
                    }
                } else if (guestSessionId != null) {
                    Mono.fromRunnable<Void> {
                        chatLogService.chatGuestAssistantMessage(
                            sessionId = guestSessionId,
                            content = chatResponse.answer,
                            queryKeywords = queryKeywords,
                            relatedMovieCds = relatedMovieCds
                        )
                    }
                } else {
                    Mono.empty()
                }

                // 응답 저장은 비동기로 실행하고, 결과를 기다릴 필요 없이 구독만 함
                saveAssistantMessageMono
                    .subscribeOn(Schedulers.boundedElastic())
                    .subscribe()
            }
            .map { (chatResponse, _, _) -> chatResponse }
            .doOnTerminate {
                val elapsed = System.currentTimeMillis() - startTime
                log.info("사용자 질문 요청 - 최종 응답 객체 생성까지 걸리는 시간 : {}ms", elapsed)
            }
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