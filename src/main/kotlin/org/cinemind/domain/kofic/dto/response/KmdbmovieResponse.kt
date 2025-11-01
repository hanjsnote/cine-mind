package org.cinemind.domain.kofic.dto.response

// KMDb API 최상위 응답 객체
data class KmdbmovieResponse(
    val Data: List<KmdbDataContainer>
)

// Data 리스트 내부 컨테이너
data class KmdbDataContainer(
    val Result: List<KmdbResult>
)

// KMDb API 영화 상세 정보
data class KmdbResult(
    // KMDb 영화 상세 정보
    val movieId: String,
    val title: String,
    val titleEng: String?,
    val releaseDate: String?,

    // List 형태 목록 필드
    val plots: PlotKMDb,
)

// plot 리스트 컨테이너
data class PlotKMDb(
    val plot: List<PlotInfo>
)

// 가장 안쪽 줄거리 정보 객체
data class PlotInfo(
    val plotLang: String,   // 줄거리 언어 (예: 한국어)
    val plotText: String    // 줄거리
)