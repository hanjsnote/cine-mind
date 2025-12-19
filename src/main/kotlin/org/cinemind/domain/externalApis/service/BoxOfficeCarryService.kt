package org.cinemind.domain.externalApis.service

import org.cinemind.domain.externalApis.client.KoficApiClient
import org.cinemind.domain.externalApis.dto.response.BoxOfficeInfo
import org.cinemind.domain.movie.repository.BoxOfficeRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.atomic.AtomicInteger
/**
 * KOFIC 주말 박스오피스의 데이터를 특정 날짜부터 호출하는 클래스
 */
@Service
class BoxOfficeCarryService (
    private val koficApiClient: KoficApiClient,
    private val boxOfficeRepository: BoxOfficeRepository
) {
    private val log = LoggerFactory.getLogger(BoxOfficeCarryService::class.java)
    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd")

    fun syncPopularMovies(): Map<String, List<BoxOfficeInfo>> {
        val startDate = LocalDate.of(2025, 11, 15)
        val today = LocalDate.now()
        val latestSunday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
        var currentDate = latestSunday.minusDays(7)

        val startDtStr = startDate.format(DATE_FORMATTER)
        val endDtStr = currentDate.format(DATE_FORMATTER)
        val existingTargetDts = boxOfficeRepository.findExistingTargetDtsBetween(startDtStr, endDtStr)

        val resultDataMap = LinkedHashMap<String, List<BoxOfficeInfo>>()
        val uniqueMovieCds = mutableSetOf<String>()
        var totalWeeks = 0

        log.info("--- 1단계: KOFIC API로부터 주간 데이터 수집 시작 ---")

        while (currentDate.isAfter(startDate)) {
            val targetDt = currentDate.format(DATE_FORMATTER)

            // 해당 주차에 이미 데이터가 있으면 스킵
            val shouldSkip = existingTargetDts.contains(targetDt)
            if (shouldSkip) {
                if (totalWeeks % 50 == 0) log.info("[{}] 이미 DB에 존재 → 스킵", targetDt)
            } else {
                try {
                    val boxOfficeList = koficApiClient.getMovieBoxOffice(targetDt)
                    if (boxOfficeList.isNotEmpty()) {
                        resultDataMap[targetDt] = boxOfficeList
                        boxOfficeList.forEach { uniqueMovieCds.add(it.movieCd) }
                    }
                } catch (e: Exception) {
                    log.error("[{}] API 수집 중 오류: {}", targetDt, e.message)
                }
            }

            // 50주마다 로그 출력
            if (totalWeeks % 50 == 0) {
                log.info("  [{} 주] 현재까지 고유 MovieCd 총계: {}", targetDt, uniqueMovieCds.size)
            }
            totalWeeks++

            // 안전장치: 최대 5000건만 저장되도록 설정
            currentDate = currentDate.minusDays(7)
            if (uniqueMovieCds.size >= 5000) break
        }

        log.info("--- 수집 완료: 총 {}주차, 고유 영화 {}건 확보 ---", resultDataMap.size, uniqueMovieCds.size)
        return resultDataMap
    }
}
