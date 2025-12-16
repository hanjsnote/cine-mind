package org.cinemind.domain.externalApis.dto.response

// BoxOffice 응답 최상위 객체
data class BoxOfficeResponse (
    val boxOfficeResult: BoxOfficeResult
)

// 주간/주말 박스 오피스 결과 컨테이너
data class BoxOfficeResult (
    val boxofficeType: String,  // 박스오피스 유형 (일별, 주말)
    val showRange: String,       // 조회 기간
    val yearWeekTime: String? = null,

    // 주간/주말 박스오피스 목록
    val weeklyBoxOfficeList: List<BoxOfficeInfo>? = null
)

// 박스 오피스 목록의 각 영화 항목 DTO
data class BoxOfficeInfo(
    val movieCd: String,        // 영화코드
    val rank: String,           // 해당 일자 박스오피스 순위
    val salesAcc: String,       // 누적 매출액
    val audiAcc: String         // 누적 관객수
)