package org.cinemind.domain.rag.client

import org.cinemind.config.openai.OpenAiConfigProperties
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

/**
 * OpenAI의 임베딩 API (text-embedding-3-small)와 통신하는 클라이언트
 **/
@Component
class RagEmbeddingClient (
    private val webClientBuilder: WebClient.Builder,
    private val openAiConfigProperties: OpenAiConfigProperties
) {
    // 임베딩 모델 정의
    private val EMBEDDING_MODEL = "text-embedding-3-small"

    fun getEmbedding(text: String): FloatArray {
        if (text.isBlank()) {
            return floatArrayOf()
        }

        // RagIndexingService에서 전달 받은 영화의 메타 정보(metaText) 또는 줄거리 텍스트를 벡터로 변환 요청
        val requestBody = mapOf(
            "input" to text,
            "model" to EMBEDDING_MODEL
        )

        // WebClient는 매 요청마다 빌더를 통해 생성
        val webClient = webClientBuilder.baseUrl(openAiConfigProperties.embeddingUrl).build()

        return webClient.post()
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${openAiConfigProperties.key}")
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(OpenAiEmbeddingResponse::class.java)
            .map { response ->
                // 첫 번째 임베딩 결과를 PostgreSQL vector 형식 문자열로 변환
                response.data.firstOrNull()?.embedding
                    ?.map { it.toFloat() }
                    ?.toFloatArray() ?: floatArrayOf()
            }
            .onErrorResume { e ->
                // API 통신 에러 발생 시
                println("OpenAI Embedding API 통신 오류: ${e.message}")
                Mono.just(floatArrayOf())   //오류 발생 시 빈 문자열 반환
            }
            .block() ?: floatArrayOf()  // Mono 블로킹 (동기 호출)
    }
}

// 내부 DTO // OpenAI API와 데이터를 주고받는 통신을 위한 DTO
private data class OpenAiEmbeddingResponse(
    val data: List<EmbeddingData>
)
// 내부에서 데이터를 전달하고 처리하기 위한 DTO
private data class EmbeddingData(
    val embedding: List<Double> // 임베딩 벡터 데이터 (숫자 배열)
)
