package org.cinemind.domain.kofic.client

import org.cinemind.domain.kofic.dto.response.KmdbMovieResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono

// KMDb API와 통신을 담당하는 클라이언트
@Component
class KmdbApiClient (
    @Value("\${kmdb.api.key}")
    private val serviceKey: String,
    private val webClientBuilder: WebClient.Builder
){
    private val baseUrl = "http://api.koreafilm.or.kr/openapi-data2/wisenut"
    private val webClient = webClientBuilder.baseUrl(baseUrl).build()

    // 영화 제목과 개봉 날짜를 기준으로 KMDbd에서 영화 상세 정보를 조회
    fun getKmdbMovieDetail(title: String, releaseDts: String): KmdbMovieResponse? {
        // 영화 제목으로 검색할 때 공백이 붙는 경우 대비해 trim()을 사용
        val uri = "/search_api/search_json2.jsp"
        val searchTitle = title.trim()

        return webClient.get()
            .uri {
                builder -> builder.path(uri)
                // KMDb에선 API Key를 ServiceKey로 사용
                .queryParam("ServiceKey", serviceKey)
                // 필수 파라미터: 검색 대상이 "film"임을 지정
                // KMDb API 검색 대상(listNaem)에서 영화(film)
                .queryParam("collection", "kmdb_new2")
                // 상세 정보를 요청하는 detail=Y 파라미터 추가
                .queryParam("detail", "Y")
                // 개봉일자(releaseDts)로 검색 (YYYYMMDD)
                .queryParam("releaseDts", releaseDts)
                .queryParam("title", searchTitle)
                .build()
            }
            .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .retrieve()
            .bodyToMono(KmdbMovieResponse::class.java)
            .onErrorResume { e ->
                println("KMDB API Response Parsing Failed (Returning null): ${e.message}")
                Mono.empty()
            }
            .block()
    }
}


