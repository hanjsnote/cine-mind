package org.cinemind.domain.externalApis.dto.etc

import org.cinemind.domain.externalApis.dto.response.KmdbResult

class MatchDetail (
    val kmdbResult: KmdbResult,         // KMDb 원본데이터
    val normalizedkmdbTitle: String,    // 정규화된 KMDB 제목
    val titleScore: Double,             // 제목 유사도 점수 (Jaro-winkler)
    val totalScore: Double,             // 최종 총 점수 (유사도 + 연도/보너스)
    val yearMatch: Boolean,             // 연도 일치 여부
    val isHighScoreBonusApplied: Boolean // 완벽 일치 보너스 적용 여부
)