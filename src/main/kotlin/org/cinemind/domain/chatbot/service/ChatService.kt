package org.cinemind.domain.chatbot.service

import org.cinemind.domain.chatbot.client.OpenAiClient
import org.cinemind.domain.movie.entity.Movie
import org.cinemind.domain.movie.repository.MovieRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

/**
 * 챗봇의 핵심 비즈니스 로직(RAG 오케스트레이션)을 담당하는 서비스
 * */
@Service
class ChatService (
    private val openAiClient: OpenAiClient,
    private val movieRepository: MovieRepository
//    private val chatCacheService: ChatCacheService    // 캐싱은 나중에 추가
){
    // 챗봇 페르소나 및 답변 규칙 정의
    private val SYSTEM_INSTRUCTION = """
        당신은 '시네마인드'의 전문 영화 추천 및 정보 제공 챗봇입니다.
        사용자의 질문에 대해 항상 친절하고 정확하게 답변해야 합니다.
        
        1. 답변시에는 제공된 [CONTEXT] 정보를 최우선으로 활용해야 합니다.
        2. [CONTEXT]에 없는 정보나 모르는 내용은 절대 지어내지 말고, "제가 가진 영화 정보에는 해당 내용이 없습니다."라고 정중하게 말하세요.
        3. 답변은 사용자가 영화에 흥미를 느낄 수 있도록 매력적이고 간결하게 작성해주세요.
        4. 답변 형식은 항상 한국어로 작성해야 합니다.
    """.trimIndent()

//    RAG Context 확보, 최종 프롬프트 생성, LLM 호출을 통합
//    userQuery 사용자 질문, LLM이 생성한 응답 텍스트를 리턴
    fun getLLMResponse(userQuery: String): Mono<String> {
        // (RAG 1단계 - 검색) 사용자 질문에서 검색 키워드를 추출하고 DB에서 관련 정보 찾기
        val contextMovies = getContextFromMovieDB(userQuery)

        // Context 기반으로 최종 프롬프트(userQuery) 생성
        val fullPrompt = createFullPrompt(userQuery, contextMovies)

        // (LLM 호출) 최종 프롬프트를 LLM 클라이언트에 전달하여 응답을 받는다.
        return openAiClient.getChatCompletion(SYSTEM_INSTRUCTION, fullPrompt)
    }

    // 임시 RAG 로직 나중에 벡터 검색으로 대체
    // 사용자 질문에 가장 관련 있는 영화 정보를 DB(MovieRepository)에서 찾는다.
    private fun getContextFromMovieDB(userQuery: String): List<Movie> {

        return TODO("반환 값을 제공하세요")
    }

    // LLM에게 전달할 최종 프롬프트를 생성
    // 시스템 지시문 + [CONTEXT] + 사용자 질문의 구조를 가짐
    private fun createFullPrompt(userQuery: String, contextMovies: List<Movie>): String{

        return TODO("반환 값을 제공하세요")
    }

}