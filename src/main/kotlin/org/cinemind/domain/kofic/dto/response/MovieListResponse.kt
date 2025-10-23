package org.cinemind.domain.kofic.dto.response

data class MovieListResponse (
    val totCont: Int,    // 전체 결과 수
    val movieList: List<MovieListItem>
)

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