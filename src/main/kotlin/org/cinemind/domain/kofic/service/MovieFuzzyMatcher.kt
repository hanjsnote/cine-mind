package org.cinemind.domain.kofic.service

import org.cinemind.domain.kofic.dto.response.KmdbMovieResponse
import org.cinemind.domain.kofic.dto.response.KmdbResult
import org.springframework.stereotype.Component

/**
 * 영화 제목, 개봉일 기준으로 퍼지 매칭(Fuzzy Matching) 로직을 담당하는 컴포넌트
 * */
@Component
class MovieTitleFuzzyMatcher {

    private val MIN_TITLE_SCORE_THRESHOLD = 0.8 // 최소 제목 유사도 점수

    // KMDb 결과 목록(Result) 중 KOFIC의 제목과 개봉일(openDt)에 가장 일치하는 영화를 찾는다.
    // 제목 유사도와 연도 일치 여부를 종합적으로 고려하여 점수를 매긴다.

    fun findBestMatch(koficTitle: String, koficOpenDt: String, kmdbResponse: KmdbMovieResponse?): KmdbResult? {
        val kmdbResults = kmdbResponse
            ?.Data?.firstOrNull()
            ?.Result
            ?: return null

        if (kmdbResults.isEmpty()) {
            return null
        }

        val nomalizedKoficTitle = normalizeTitle(koficTitle)

        var bestMatch: KmdbResult? = null
        var highestTotalScore = -1.0 // 최종 매칭 점수 (제목 유사도 + 연도 가산점)

        for (candidate in kmdbResults) {
            val normalizedKmdbTitle = normalizeTitle(candidate.title)

            // 제목 유사도 점수 계산
            val titleScore = calculateSimilarityScore(nomalizedKoficTitle, normalizedKmdbTitle)

            // 최소 제목 유사도 기준 미달 시 다음 후보로 이동
            if (titleScore < MIN_TITLE_SCORE_THRESHOLD) {
                continue
            }

            // 연도 일치 가산점 계산
            var yearBonus = 0.0

            // KOFIC 개봉일(OpenDt)에서 연도만 추출
            val koficYear = koficOpenDt.substring(0, 4)

            // KMDb 개봉일(releaseDts)에서 연도만 추출
            val kmdbYear = candidate.releaseDts?.substring(0, 4)

            // KMDb의 prodYear가 KOFIC 연도와 정확히 일치하면 큰 가산점 부여
            if (kmdbYear == koficYear) {
                yearBonus = 0.5
            }
            // 최종 점수 = 제목 유사도 + 연도 보너스

            val totalScore = titleScore + yearBonus

            if (totalScore > highestTotalScore) {
                highestTotalScore = totalScore
                bestMatch = candidate
            }
        }
        return bestMatch
    }

    // 영화 이름을 정규화: 공백 제거, 소문자 변환 등
    private fun normalizeTitle(title: String): String {
        return title.replace("\\s".toRegex(), "")   // 공백 제거
            .replace("[^\\w가-힣]".toRegex(), "")    // 특수문자 제거 (한글, 영문, 숫자 외)
            .lowercase()    // 소문자 변환
    }

    // 두 정규화된 영화 이름간의 유사도 점수를 계산
    private fun calculateSimilarityScore(koficMovieNm: String, kmdbTitle: String): Double {
        val len1 = koficMovieNm.length
        val len2 = kmdbTitle.length
        val maxLen = Math.max(len1, len2).toDouble()

        // 두 문자열 모두 비어있지 않고 하나가 다른 하나를 포함하는 경우 유사도 계산
        if (maxLen > 0 && (koficMovieNm.contains(kmdbTitle) || kmdbTitle.contains(koficMovieNm))) {
            // 길이 차이가 적을수록 높은 점수
            return 1.0 - (Math.abs(len1 - len2).toDouble() / maxLen)
        }
        return 0.0
    }
}