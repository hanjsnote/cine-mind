package org.cinemind.domain.chatbot.client

import com.fasterxml.jackson.databind.ObjectMapper
import org.cinemind.config.openai.OpenAiConfigProperties
import org.cinemind.domain.chatbot.dto.message.Message
import org.cinemind.domain.chatbot.dto.request.ChatRequest
import org.cinemind.domain.chatbot.dto.response.OpenAiChatCompletion
import org.cinemind.domain.chatbot.enum.MessageRole
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
/**
 * 외부 API 통신을 위한 WebClient 설정 파일.
 */
@Component
class OpenAiClient(
    private val webClientBuilder: WebClient.Builder,
    private val openAiConfigProperties: OpenAiConfigProperties,
    // JSON 파싱을 위해 ObjectMapper 주입
    private val objectMapper: ObjectMapper
) {
    // LLM 모델 정보 정의
    private val LLM_MODEL = "gpt-4o-mini"
    private val API_PATH = "" // yml에 전체 경로가 정의되어 있으므로 여기선 "" 사용

    // LLM에 질문(프롬프트)을 전송하고 답변을 받는다
    // RAG의 Context와 사용자 질문이 합쳐진 형태가 userQuery로 전달
    fun <T : Any>getChatCompletion(systemInstruction: String, userQuery: String, responseClass: Class<T>): Mono<T> {
        // ChatRequest를 구성할 때 LLM이 JSON을 출력하도록 강제
        val request = ChatRequest(
            model = LLM_MODEL,
            messages = listOf(
                // 시스템 메시지와 사용자 메시지를 DTO로 구성
                Message(role = MessageRole.SYSTEM, content = systemInstruction),
                Message(role = MessageRole.USER, content = userQuery)
            )
        )

        // 설정 정보를 사용하여 WebClient 인스턴스를 빌드
        val webClient = webClientBuilder.baseUrl(openAiConfigProperties.chatUrl).build()

        return webClient.post()
            .uri(API_PATH)
            .header("Authorization", "Bearer ${openAiConfigProperties.key}")
            .bodyValue(request)
            .retrieve()
            .bodyToMono<OpenAiChatCompletion>()
            .map { response ->
                // 첫 번째 선택지 (choices[0]의 content를 반환)
                val jsonString = response.choices.firstOrNull()?.message?.content
                    ?: throw RuntimeException("LLM 응답에서 JSON 문자열을 찾을 수 없습니다.")

                objectMapper.readValue(jsonString, responseClass)
            }
            .onErrorResume { e ->
                // API 통신 에러 발생 시
                System.err.println("OpenAI API 통신 또는 JSON 파싱 오류: ${e.message}")
                Mono.error(RuntimeException("LLM 응답 처리 중 오류가 발생했습니다.", e))
            }
    }
}

