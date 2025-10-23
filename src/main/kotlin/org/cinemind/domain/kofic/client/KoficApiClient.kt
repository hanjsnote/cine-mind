package org.cinemind.domain.kofic.client

import org.cinemind.domain.kofic.dto.response.BoxOfficeInfo
import org.cinemind.domain.kofic.dto.response.BoxOfficeResponse
import org.cinemind.domain.kofic.dto.response.MovieInfo
import org.cinemind.domain.kofic.dto.response.MovieInfoResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import kotlin.collections.List

// API 키와 URL을 사용하여 외부 호출을 담당
@Component
class KoficApiClient (
    @Value("\${KOFIC_API_KEY}")
    private val apiKey: String,
    // WebClient를 사용하기 위해 WebClient.Builder를 주입
    private val webClientBuilder: WebClient.Builder
){
    private val baseUrl = "http://www.kobis.or.kr/kobisopenapi/webservice/rest"
    private val webClient = webClientBuilder.baseUrl(baseUrl).build()

    // movieCd와 BoxOffice 통계 정보를 가져오는 API (단일 날짜)
    fun getMovieBoxOffice(targetDt: String): List<BoxOfficeInfo> {
        val uri = "/boxoffice/searchDailyBoxOfficeList.json"

        // WebClient를 이용한 호출
        return webClient.get()
            .uri {
                builder -> builder.path(uri)
                .queryParam("key", apiKey)
                .queryParam("targetDt", targetDt)
                .build()
            }
            // 요청 실행
            .retrieve()
            // 서버로부터 받은 JSON 응답을 미리 정의된 DTO 객체로 변환
            .bodyToMono(BoxOfficeResponse::class.java)
            // JSON 응답구조 필터링 null이면 빈 리스트 반환
            .block()?.boxOfficeResult?.dailyBoxOfficeList?: emptyList()
    }

    // 영화 상세 정보 API 단일 movieCd에 대한 상세 정보를 DTO로 가져옴
    fun getMovieDetailList(movieCd: String): MovieInfo? {
        val uri = "/movie/searchMovieInfo.json"

        // WebClient를 이용한 호출
        return webClient.get()
            .uri {
                builder -> builder.path(uri)
                .queryParam("key", apiKey)
                .queryParam("movieCd", movieCd)
                .build()
            }
            // 요청 실행
            .retrieve()
            // 서버로부터 받은 JSON 응답을 미리 정의된 DTO 객체로 변환
            .bodyToMono(MovieInfoResponse::class.java)
            // JSON 응답구조 필터링
            .block()?.movieInfoResult?.movieInfo
    }
}