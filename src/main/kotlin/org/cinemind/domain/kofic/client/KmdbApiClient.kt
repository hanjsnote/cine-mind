package org.cinemind.domain.kofic.client

import com.fasterxml.jackson.databind.ObjectMapper // 1. ObjectMapper 임포트
import org.cinemind.domain.kofic.dto.response.KmdbMovieResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono

// KMDb API와 통신을 담당하는 클라이언트
@Component
class KmdbApiClient (
    @Value("\${kmdb.api.key}")
    private val serviceKey: String,
    private val webClientBuilder: WebClient.Builder,
    // 2. ObjectMapper 주입
    private val objectMapper: ObjectMapper
){
    private val baseUrl = "http://api.koreafilm.or.kr/openapi-data2/wisenut"

    private val webClient: WebClient = webClientBuilder.baseUrl(baseUrl)
        .defaultHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .build()

    // 영화 제목 기준으로 KMDb에서 영화 상세 정보를 조회
    fun getKmdbMovieDetail(title: String, releaseDate: String?): KmdbMovieResponse? {
        val uriPath = "/search_api/search_json2.jsp"
        val searchTitle = title.trim()

        val finalUri = UriComponentsBuilder.fromHttpUrl(baseUrl + uriPath)
            .queryParam("ServiceKey", serviceKey)
            .queryParam("collection", "kmdb_new2")
            .queryParam("detail", "Y")
            .queryParam("query", searchTitle)
            .apply { if (!releaseDate.isNullOrBlank()) queryParam("releaseDts", releaseDate) }
            .encode()
            .toUriString()

        println("KMDB Request URI: $finalUri")

        // 응답을 Content-Type과 상관없이 String으로 받기
        val rawResponseMono = webClient.get()
            .uri { builder ->
                builder.path(uriPath)
                    .queryParam("ServiceKey", serviceKey)
                    .queryParam("collection", "kmdb_new2")
                    .queryParam("detail", "Y")
                    .queryParam("query", searchTitle)
                    .apply { if (!releaseDate.isNullOrBlank()) queryParam("releaseDts", releaseDate) }
                    .build()
            }
            .retrieve()
            // Content-Type에 관계없이 응답 본문을 String으로 받음 (핵심 우회!)
            .bodyToMono(String::class.java)
            .onErrorResume { e ->
                println("KMDB API Connection/Receive Error: ${e.message}")
                Mono.just("")
            }

        val rawResponseBody = try {
            rawResponseMono.block() ?: ""
        } catch (e: Exception) {
            println("KMDB API Block Exception: ${e.message}")
            return null
        }

        if (rawResponseBody.isBlank()) {
            println("KMDB: [$searchTitle] 빈 응답 본문.")
            return null
        }

        // 수신된 String 본문을 ObjectMapper를 사용하여 DTO로 강제 변환
        val kmdbResponse = try {
            // 응답이 JSON이 아니라 HTML 오류 페이지인지 한 번 더 확인 (ServiceKey 오류 등 대비)
            if (rawResponseBody.trimStart().startsWith("<")) {
                println("KMDB: [$searchTitle] HTML 응답이 의심됩니다. 본문 미리보기: ${rawResponseBody.take(100)}")
                println("ServiceKey [$serviceKey]가 유효한지 다시 확인해주세요. 서버에서 오류 페이지(HTML)를 보냈습니다.")
                return null
            }

            // JSON으로 간주하고 DTO로 강제 파싱 (성공적인 데이터 수신)
            objectMapper.readValue(rawResponseBody, KmdbMovieResponse::class.java)
        } catch (e: Exception) {
            // 파싱 오류 발생 (본문이 유효한 JSON이 아닌 경우)
            println("KMDB API Response Failed during ObjectMapper readValue. Cause: ${e.message}")
            println("Raw Response (Preview): ${rawResponseBody.take(500)}")
            return null
        }

        return kmdbResponse
    }
}