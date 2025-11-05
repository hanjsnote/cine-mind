package org.cinemind.domain.externalApis.dto.response

import com.fasterxml.jackson.annotation.JsonProperty

// Kofic API 응답의 최상위 객체
data class MovieInfoResponse (
    val movieInfoResult: MovieInfoResult
)

data class MovieInfoResult(
    val movieInfo: MovieInfo
)

// API에서 가져오는 영화 상세 정보 DTO
data class MovieInfo(
    val movieCd: String,        // 영화코드
    val movieNm: String,        // 영화명(국문)
    val movieNmEn: String,      // 영화명(영문)
    val showTm: String,         // 상영시간
    val openDt: String,         // 개봉일
    val typeNm: String,         // 영화유형

    // List 형태 목록 필드
    val genres: List<GenreKofic>,
    val directors: List<DirectorKofic>,
    val actors: List<ActorKofic>,
    val companys: List<CompanyKofic>,
    val audits: List<AuditKofic>
)

// 배열 객체들을 위한 DTO
data class GenreKofic(
    val genreNm: String         // 장르명
)

data class DirectorKofic(
    val peopleNm: String,     // 감독 이름(국문)
    val peopleNmEn: String?     // 감독 이름(영문)
)

data class ActorKofic(
    val peopleNm: String,      // 배우 이름(국문)
    val peopleNmEn: String?,     // 배우 이름(영문)
    @JsonProperty("cast") // API 응답 필드인 cast와 다르기에 명시적 매핑
    val castNm: String,          // 배역명
)

data class CompanyKofic(
    val companyCd: String,       // 참여 영화사 코드
    val companyNm: String,       // 참여 영화사(국문)
    val companyNmEm: String?,    // 참여 영화사(영문)
    val companyPartNm: String    // 참여 영화사 분야명
)

data class AuditKofic(
    val watchGradeNm: String    // 관람등급
)


