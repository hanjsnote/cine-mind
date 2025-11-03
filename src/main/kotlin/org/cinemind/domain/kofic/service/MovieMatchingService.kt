package org.cinemind.domain.kofic.service

import org.cinemind.domain.kofic.dto.response.KmdbMovieResponse
import org.cinemind.domain.kofic.dto.response.KmdbResult
import org.springframework.stereotype.Component

/**
 * 영화 제목, 개봉일 기준으로 퍼지 매칭(Fuzzy Matching) 로직을 담당하는 컴포넌트
 * */
@Component
class MovieMatchingService {
    // Jaro-Winkler 유사도 기준에 맞춰 임계값을 0.85 조정
    private val MIN_TITLE_SCORE_THRESHOLD = 0.85

    // KMDb 결과 목록(Result) 중 KOFIC의 제목과 개봉일(openDt)에 가장 일치하는 영화를 찾는다.
    // 제목 유사도와 연도 일치 여부를 종합적으로 고려하여 점수를 매긴다.

    fun findBestMatch(koficTitle: String, kmdbResponse: KmdbMovieResponse?): KmdbResult? {
        val kmdbResults = kmdbResponse
            ?.Data?.firstOrNull()
            ?.Result
            ?: return null

        if (kmdbResults.isEmpty()) {
            return null
        }

        // 정규화 시 불필요한 공백을 제거하고 비교
        val nomalizedKoficTitle = normalizeTitle(koficTitle)

        var bestMatch: KmdbResult? = null
        var highestTotalScore = -1.0 // 초기 점수

        for (candidate in kmdbResults) {
            val normalizedKmdbTitle = normalizeTitle(candidate.title)

            // 제목 유사도 점수 계산 (Jaro-Winkler) 계산
            val titleScore = calculateSimilarityScore(nomalizedKoficTitle, normalizedKmdbTitle)

            // 최소 제목 유사도 기준 미달 시 다음 후보로 이동
            if (titleScore < MIN_TITLE_SCORE_THRESHOLD) {
                continue
            }

            // 최종 점수
            if (titleScore > highestTotalScore) {
                highestTotalScore = titleScore
                bestMatch = candidate
            }
        }
        return bestMatch
    }

    // 영화 이름을 정규화: 공백 제거, 소문자 변환 등
    private fun normalizeTitle(title: String): String {
        // "어벤져서: 엔드게임"같이 부제를 비교할 수 있도록 콜론(:)이나 하이픈(-) 등은 제거하지 않고 공백을 제거 후 소문자 변환
        return title.trim().replace("\\s".toRegex(), "").lowercase()
    }

    // 두 정규화된 영화 이름간의 유사도 점수를 Jaro-Winkler 알고리즘으로 계산
    // @param s1 첫 번째 정규화된 제목
    // @param s2 두 번째 정규화된 제목
    private fun calculateSimilarityScore(s1: String, s2: String): Double {
        if (s1 == s2) return 1.0
        if (s1.isEmpty() || s2.isEmpty()) return 0.0

        val maxLen = Math.max(s1.length, s2.length)
        val matchingWindow = Math.max(0, maxLen / 2 - 1)

        // 매칭 문자 찾기 matches2[j]가 false이면 s2의 j번째 문자가 아직 s1의 어떤 문자도 짝지어지지 않았다는 뜻 (사용가능)
        val matches1 = BooleanArray(s1.length) { false }    // 배열의 초기값은 전부 false
        val matches2 = BooleanArray(s2.length) { false }
        var m = 0   // 매칭된 문자 수

        for (i in s1.indices) {
            // start가 음수일 경우 Math.max(0...) 항상 0으로 음수가 되는것을 방지
            val start = Math.max(0, i - matchingWindow)
            val end = Math.min(s2.length - 1, i + matchingWindow)

            for (j in start..end) {
                if (!matches2[j] && s1[i] == s2[j]) {
                    matches1[i] = true
                    matches2[i] = true
                    m++
                    break
                }
            }
        }

        if (m == 0) return 0.0

        // 2. Transposition (전치) 계산
        val ms1 = StringBuilder()
        val ms2 = StringBuilder()

        for (i in s1.indices) {
            if (matches1[i])
                ms1.append(s1[i])
        }
        for (i in s2.indices) {
            if (matches2[i])
                ms2.append(s2[i])
        }

        var t = 0 // 전치 횟수
        for (i in 0 until m) {
            // 두 매칭 문자열의 순서가 다를 때 전치로 간주
            if (ms1[i] != ms2[i]) {
                t++
            }
        }
        t /= 2 // 전치는 항상 쌍으로 계산

        // Jaro 유사도 계산
        val jaroScore = (m.toDouble() / s1.length + m.toDouble() / s2.length + (m - t).toDouble() / m.toDouble()) / 3.0

        // Winkler 가산점 적용 (첫 4개 문자가 일치하면 가산점 부여)
        var p = 0
        val maxPrefix = Math.min(4, Math.min(s1.length, s2.length))
        while (p < maxPrefix && s1[p] == s2[p]) {
            p++
        }

        // Winkler 공식: jaroScore + (p ( 0.1 * (1 - jaroScore))
        val jaroWinklerScore = jaroScore + (p * 0.1 * (1.0 - jaroScore))

        return jaroWinklerScore
    }
}