package org.cinemind.domain.kofic.dto.response

// KMDb API 최상위 응답 객체
data class KmdbMovieResponse(
    val Data: List<KmdbDataContainer>
)

// Data 리스트 내부 컨테이너
data class KmdbDataContainer(
    val Result: List<KmdbResult>
)

// KMDb API 영화 상세 정보
data class KmdbResult(
    // KMDb 영화 상세 정보
    val movieId: String,    // movie_id
    val title: String,      // 영화명
    val titleEng: String?,  // 영화명(영문)

    // List 형태 목록 필드
    val plots: KmdbPlots,
    val ratings: KmdbRatings
)

// plot 리스트 컨테이너
data class KmdbPlots(
    val plot: List<PlotInfo>?
)

// 가장 안쪽 줄거리 정보 객체
data class PlotInfo(
    val plotLang: String,   // 줄거리 언어 (예: 한국어)
    val plotText: String    // 줄거리
)

data class KmdbRatings(
    val rating: List<KmdbRating>?
)

data class KmdbRating(
    val releaseDate: String?    // 개봉일
)