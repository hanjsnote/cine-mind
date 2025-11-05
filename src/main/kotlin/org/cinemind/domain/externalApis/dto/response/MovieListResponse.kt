package org.cinemind.domain.externalApis.dto.response

// 최상위 응답 객체
data class MovieListResponse(
    val movieListResult: MovieListResult
)

// 결과 컨테이너
data class MovieListResult (
    val totCnt: Int,    // 전체 결과 수
    val movieList: List<MovieListItem>
)

// 목록의 각 항목 (Movie 엔티티의 1차 정보를 담음)
data class MovieListItem(

    val movieCd: String,    // 영화코드
    val movieNm: String,    // 영화명(국문)
    val movieNmEn: String,  // 영화명(영문)
    val openDt: String,     // 개봉일
    val typeNm: String,     // 영화유형
    val genreAlt: String,    // 영화장르(전체)
    // directors와 companys는 리스트 형태
    val directors: List<DirectorInfo> = emptyList(),    // 영화감독
    val companys: List<CompanyInfo> =   emptyList()     // 제작사
)

data class DirectorInfo(
    val peopleNm: String
)

data class CompanyInfo(
    val companyCd: String,
    val companyNm: String
)