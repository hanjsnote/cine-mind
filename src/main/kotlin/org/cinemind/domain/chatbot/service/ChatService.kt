package org.cinemind.domain.chatbot.service

import org.cinemind.common.dto.authuser.AuthUser
import org.cinemind.domain.chatbot.client.OpenAiClient
import org.cinemind.domain.chatbot.dto.response.ChatLLMResponse
import org.cinemind.domain.chatbot.dto.response.LlmStructuredResponse
import org.cinemind.domain.chatlog.service.ChatLogService
import org.cinemind.domain.rag.dto.model.MovieEmbeddingDto
import org.cinemind.domain.rag.service.RagRetrievalService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

/**
 * 챗봇의 핵심 비즈니스 로직(RAG 오케스트레이션)을 담당하는 서비스
 * 이 서비스는 다음 3단계를 통ㅎ바하여 실행 (R -> A -> G)
 * 1. Retrieval (검색): RagRetrievalService를 통해 Context 확보
 * 2. Augmentation (증강): 확보된 Context로 프롬프트 증강
 * 3. Generation (생성): OpenAiClient를 통해 답변 생성
 * */
@Service
class ChatService (
    private val openAiClient: OpenAiClient,
    private val ragRetrievalService: RagRetrievalService,
    private val chatLogService: ChatLogService
//    private val chatCacheService: ChatCacheService    // 캐싱은 나중에 추가
){
    private val log = LoggerFactory.getLogger(javaClass)

    // 챗봇 페르소나 및 답변 규칙 정의
    private val SYSTEM_INSTRUCTION = """
        당신은 '시네마인드'의 전문 영화 추천 및 정보 제공 챗봇입니다.
        사용자의 질문에 대해 항상 친절하고 정확하게 답변해야 합니다.
        
        **[출력 형식 규칙]**
        **당신은 어떠한 추가 설명 없이 오직 JSON 형식의 객체 하나만 출력해야 합니다.**
        JSON은 반드시 다음 스키마를 따라야 하며 'queryKeywords' 필드에는 캐시 키 생성을 위해 다음과 같은 핵심 키워드를 추출해야 합니다.
        
        **[키워드 추출 규칙 (캐시 정규화 목적)]**
        1. 추출 키워드는 질문의 **가장 핵심적인 3개의 고유 명사나 주제어**만 포함합니다. (예: 인물, 영화 제목, 특정 사건, 핵심 개념)
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
    // userQuery 사용자 질문, LLM이 생성한 응답 텍스트를 리턴
    fun getLLMResponse( authUser: AuthUser?, userQuery: String): Mono<ChatLLMResponse> {

        // 대화 내역 저장 LLM 호출 전에 사용자 메시지를 먼저 저장
        authUser?.let { chatLogService.saveUserMessage(it.id, userQuery) }

        // 요청 시작 시간 측정
        val startTime = System.currentTimeMillis()

        // (RAG 1단계 - 검색) 사용자 질문을 벡터화하여 가장 관련성이 높은 Context 청크를 검색
        val contextChunks = ragRetrievalService.retrieveRelevantContext(userQuery)

        // Context 기반으로 최종 프롬프트(userQuery) 생성
        val fullPrompt = createFullPrompt(userQuery, contextChunks)

        // (LLM 호출) 최종 프롬프트와 JSON 출력 지침을 클라이언트에 전달하여 StructuredResponse를 받는다
        val structuredResponseMono: Mono<LlmStructuredResponse> = openAiClient.getChatCompletion(
            SYSTEM_INSTRUCTION,
            fullPrompt,
            LlmStructuredResponse::class.java   // JSON 응답을 이 내부 DTO로 파싱하도록 지시
        )

        // LLM 응답을 최종 DTO와 로깅 키워드(Pair)로 변환
        return structuredResponseMono
            .map { structuredResponse ->
                // 검색 근거로 사용된 텍스트 청크를 리스트로 구성
                val sources = contextChunks.map {
                    // 메타 정보와 줄거리를 구분하여 근거로 사용 (디버깅용)
                    "[메타데이터] ${it.metaText}\n[줄거리] ${it.plotText}"
                }
                // 최종 사용자에게 반환될 응답 DTO
                val chatResponse = ChatLLMResponse(
                    answer = structuredResponse.answer,
                    sources = sources
                )
                // 반환할 응답과 로깅에 필요한 키워드를 Pair로 묶어 다음 체인에 전달
                Pair(chatResponse, structuredResponse.queryKeywords)
            }
            .doOnSuccess { (chatResponse, queryKeywords) ->
                // 챗봇 응답 저장 Mono의 결과가 성공적으로 생성 되었을때 DB 저장
                val relatedMovieCds = contextChunks.map { it.movieCd }

                authUser?.let {
                    chatLogService.chatAssistantMessage(
                        userId = it.id,
                        content = chatResponse.answer,
                        queryKeywords = queryKeywords,
                        relatedMovieCds = relatedMovieCds
                    )
                }
            }
            //최종적으로 반환할 ChatLLMResponse만 추출
            .map { (chatResponse, _) -> chatResponse }

            // 사용자 질문 요청 ~ 최종 응답 객체 생성까지 걸리는 시간
            .doOnTerminate {
                val elapsed = System.currentTimeMillis() - startTime
                log.info("사용자 질문 요청 - 최종 응답 객체 생성까지 걸리는 시간 : {}ms", elapsed)
            }
    }

    // LLM에게 전달할 최종 프롬프트를 생성Å
    // 시스템 지시문 + [CONTEXT] + 사용자 질문의 구조를 가짐
    private fun createFullPrompt(userQuery: String, contextChunks: List<MovieEmbeddingDto>): String{
        // contextChunks 없으면 빈 문자열을, 있으면 형식화된 영화 정보를 포함
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
        return """
        [CONTEXT]
         $contextString

        [사용자 질문]
        $userQuery
        """.trimIndent()
    }
}