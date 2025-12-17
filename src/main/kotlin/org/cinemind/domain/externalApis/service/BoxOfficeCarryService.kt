package org.cinemind.domain.externalApis.service

import org.cinemind.domain.externalApis.client.KoficApiClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicInteger
/**
 * KOFIC 주말 박스오피스의 데이터를 특정 날짜부터 호출하는 클래스
 */
@Service
class BoxOfficeCarryService (
    private val koficApiClient: KoficApiClient,
    private val boxOfficeBatchService: BoxOfficeBatchService
) {
    private val log = LoggerFactory.getLogger(BoxOfficeCarryService::class.java)
    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd")
    private val BATCH_SIZE = 100 // 배치 사이즈

    // 2004년도부터 주말 박스 오피스 순회하여 고유 MovieCd를 수집
    // 수집된 MovieCd 목록을 100개 단위로 나누어 배치 처리 위임
    fun syncPopularMovies() {
        val startDate = LocalDate.of(2004, 1, 1) // 데이터 수집 시작일
        val endDate = LocalDate.now().minusDays(7) // 최근 1주 전까지
        val uniqueMovieCds = LinkedHashSet<String>() // 순서 보장을 위한 LinkedHashSet 사용
        var currentDate = startDate
        var totalWeeks = 0

        log.info("--- 1단계: 2004년부터 현재까지 주간 박스오피스 MovieCd 수집 시작 ---")

        // 7일 간격으로 날짜를 증가시키며 주간 박스오피스 목록을 가져옴 (순차적 실행)
        while (currentDate.isBefore(endDate)) {
            val targetDt = currentDate.format(DATE_FORMATTER)

            // 주간 박스 오피스 API 호출
            val boxOfficeList = try {
                koficApiClient.getMovieBoxOffice(targetDt)
            } catch (e: Exception) {
                log.error("[{} 주] 주간 박스오피스 API 호출 중 오류 발생: {}", targetDt, e.message)
                emptyList()
            }

            if (boxOfficeList.isNotEmpty()) {
                boxOfficeList.forEach { info ->
                    uniqueMovieCds.add(info.movieCd)
                }
            }

            if (totalWeeks % 50 == 0) {
                log.info("  [{} 주] 현재까지 고유 MovieCd 총계: {}", targetDt, uniqueMovieCds.size)
            }

            currentDate = currentDate.plusDays(7)
            totalWeeks++

            // 안정 장치: 약 500건 목표 달성 시 중단
            if (uniqueMovieCds.size >= 500) {
                log.info("최대 목표치인 500건의 고유 MovieCd를 달성하여 수집을 중단합니다.")
                break
            }
        }

        log.info("--- 1단계 완료: 총 {}주 동안 {}개의 고유 MovieCd 확보. ---", totalWeeks, uniqueMovieCds.size)
        log.info("--- 2단계: 확보된 MovieCd 목록을 {}건 단위로 나누어 트랜잭션 배치 처리 시작 ---", BATCH_SIZE)

        val movieCdList = uniqueMovieCds.toList()
        val totalCount = movieCdList.size
        val totalBatches = (totalCount + BATCH_SIZE - 1) / BATCH_SIZE // 총 배치 횟수
        val batchCounter = AtomicInteger(0)

        // 100개 단위로 목록을 분할하여 처리
        movieCdList.chunked(BATCH_SIZE).forEach { batch ->
            val currentBatchNum = batchCounter.incrementAndGet()
            log.info(">> [배치 {}/{}] {}건의 MovieCd에 대한 상세 정보 저장 시작...",
                currentBatchNum, totalBatches, batch.size
            )
            try {
                // @Transactional이 적용된 배치 서비스 호출 (내부에서 병렬 처리)
                boxOfficeBatchService.saveMovieDetailBatch(batch)
                log.info(">> [배치 {}/{}] DB 트랜잭션 커밋 완료.", currentBatchNum, totalBatches)
            } catch (e: Exception) {
                log.error("!! [배치 {}/{}] 데이터 저장 중 오류 발생: {}", currentBatchNum, totalBatches, e.message, e)
            }
        }

        log.info("--- 2단계 완료: 총 {}개의 배치 처리 완료. 모든 인기 영화 데이터 적재 완료. ---", totalBatches)
    }
}

