package org.cinemind.domain.externalApis.client

import org.cinemind.domain.externalApis.dto.response.BoxOfficeInfo
import org.cinemind.domain.externalApis.dto.response.BoxOfficeResponse
import org.cinemind.domain.externalApis.dto.response.MovieInfo
import org.cinemind.domain.externalApis.dto.response.MovieInfoResponse
import org.cinemind.domain.externalApis.dto.response.MovieListResponse
import org.cinemind.domain.externalApis.dto.response.MovieListResult
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

// API 키와 URL을 사용하여 KOFIC API와 통신을 담당하는 클라이언트
@Component
class KoficApiClient (
    @Value("\${kofic.api.key}")
    private val apiKey: String,
    // WebClient를 사용하기 위해 WebClient.Builder를 주입
    private val webClientBuilder: WebClient.Builder
){
    private val baseUrl = "http://www.kobis.or.kr/kobisopenapi/webservice/rest"
    private val webClient = webClientBuilder.baseUrl(baseUrl).build()

   // 영화 목록 API
   fun getMovieList(curPage: Int, itemPerPage: Int): MovieListResult? {
       val uri = "/movie/searchMovieList.json"

       // WebClient를 이용한 호출
       return webClient.get()
           .uri {
               builder -> builder.path(uri)
               .queryParam("key", apiKey)
               .queryParam("curPage", curPage.toString())   //페이지 번호
               .queryParam("itemPerPage", itemPerPage.toString())   //페이지 당 항목 수
               .build()
           }
            // 요청 실행
           .retrieve()
           // 서버로부터 받은 JSON 응답을 미리 정의된 DTO 객체로 변환
           .bodyToMono(MovieListResponse::class.java)
           // JSON 응답구조 필터링
           .block()
           ?.movieListResult    // totCnt와 movieList가 포홤된 MovieListResult 반환
   }

    // 영화 상세 정보 API 단일 movieCd에 대한 상세 정보를 DTO로 가져옴
    fun getMovieDetailList(movieCd: String): MovieInfo? {
        val uri = "/movie/searchMovieInfo.json"

        return webClient.get()
            .uri {
                builder -> builder.path(uri)
                .queryParam("key", apiKey)
                .queryParam("movieCd", movieCd)
                .build()
            }
            .retrieve()
            .bodyToMono(MovieInfoResponse::class.java)
            .block()?.movieInfoResult?.movieInfo
    }

    // targetDt(날짜) 기준으로 일일 박스오피스 목록을 가져오는 API (단일 날짜)
    fun getMovieBoxOffice(targetDt: String): List<BoxOfficeInfo> {
        val uri = "/boxoffice/searchDailyBoxOfficeList.json"

        return webClient.get()
            .uri {
                    builder -> builder.path(uri)
                .queryParam("key", apiKey)
                .queryParam("targetDt", targetDt)
                .build()
            }
            .retrieve()
            .bodyToMono(BoxOfficeResponse::class.java)
            .block()
            ?.boxOfficeResult
            ?.dailyBoxOfficeList
            ?: emptyList()  // null이거나 목록이 없으면 빈 리스트 반환
    }
}

