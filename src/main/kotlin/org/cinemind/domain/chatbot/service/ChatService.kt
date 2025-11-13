package org.cinemind.domain.chatbot.service

import org.cinemind.common.dto.AuthUser
import org.cinemind.domain.chatbot.client.OpenAiClient
import org.cinemind.domain.chatbot.dto.response.ChatResponse
import org.cinemind.domain.chatlog.repository.ChatLogRepository
import org.cinemind.domain.chatlog.service.ChatLogService
import org.cinemind.domain.rag.dto.etc.MovieEmbeddingDto
import org.cinemind.domain.rag.service.RagRetrievalService
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
    // 챗봇 페르소나 및 답변 규칙 정의
    private val SYSTEM_INSTRUCTION = """
        당신은 '시네마인드'의 전문 영화 추천 및 정보 제공 챗봇입니다.
        사용자의 질문에 대해 항상 친절하고 정확하게 답변해야 합니다.

        1. 답변시에는 제공된 [CONTEXT] 정보를 최우선으로 활용해야 합니다.
        2. 만약 [CONTEXT]가 "제공할 Context 정보가 없습니다."로 비어 있다면, **당신의 기본 지식이나 일반 상식을 일절 활용하지 말고**, 오직 다음 문구만 출력해야 합니다: "제가 가진 영화 정보에는 해당 내용이 없습니다." 이외의 **어떠한 부가 설명도 절대 금지**합니다.
        3. [CONTEXT] 정보가 있다면, 답변은 사용자가 영화에 흥미를 느낄 수 있도록 매력적이고 간결하게 작성해주세요.
        4. 답변 형식은 항상 한국어로 작성해야 합니다.
    """.trimIndent()

    // RAG Context 확보, 최종 프롬프트 생성, LLM 호출을 통합
    // userQuery 사용자 질문, LLM이 생성한 응답 텍스트를 리턴
    fun getLLMResponse( authUser: AuthUser?, userQuery: String): Mono<ChatResponse> {

        // 대화 내역 저장 LLM 호출 전에 사용자 메시지를 먼저 저장
        authUser?.let { chatLogService.saveUserMessage(it.id, userQuery) }

        // (RAG 1단계 - 검색) 사용자 질문을 벡터화하여 가장 관련성이 높은 Context 청크를 검색
        val contextChunks = ragRetrievalService.retrieveRelevantContext(userQuery)

        // Context 기반으로 최종 프롬프트(userQuery) 생성
        val fullPrompt = createFullPrompt(userQuery, contextChunks)

        // (LLM 호출) 최종 프롬프트를 LLM 클라이언트에 전달하여 응답을 받는다.
        val llmResponseMono = openAiClient.getChatCompletion(SYSTEM_INSTRUCTION, fullPrompt)

        // LLM 응답이 오면 이를 검색된 Context와 함께 RagResponseDto로 매핑하여 반환
        return llmResponseMono
            .map { answer ->
                // 검색 근거로 사용된 텍스트 청크를 리스트로 구성
                val sources = contextChunks.map {
                    // 메타 정보와 줄거리를 구분하여 근거로 사용 (디버깅용)
                    "[메타데이터] ${it.metaText}\n[줄거리] ${it.plotText}"
                }
                ChatResponse(
                    answer = answer,
                    sources = sources
                )
            }
            .doOnSuccess { chatResponse ->
                // 챗봇 응답 저장 Mono의 결과가 성공적으로 생성 되었을때 DB 저장
                val relatedMovieCodes = contextChunks.map { it.movieCd }

                authUser?.let {
                    chatLogService.chatAssistantMessage(
                        userId = it.id,
                        content = chatResponse.answer,
                        queryKeywords = listOf(),
                        relatedMovieCodes = relatedMovieCodes
                    )
                }
            }
    }

    // LLM에게 전달할 최종 프롬프트를 생성
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