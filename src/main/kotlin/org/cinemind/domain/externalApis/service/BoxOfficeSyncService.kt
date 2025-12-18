package org.cinemind.domain.externalApis.service

import org.cinemind.domain.externalApis.dto.response.BoxOfficeInfo
import org.cinemind.domain.movie.entity.BoxOffice
import org.cinemind.domain.movie.repository.BoxOfficeRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 전체 인기 영화 및 박스오피스 동기화 메인 로직
 */
@Service
class BoxOfficeSyncService (
    private val boxOfficeRepository: BoxOfficeRepository,
    private val koficDataSyncService: KoficDataSyncService,
    private val boxOfficeCarryService: BoxOfficeCarryService,
    private val boxOfficeBatchService: BoxOfficeBatchService
){
    private val log = LoggerFactory.getLogger(BoxOfficeSyncService::class.java)

    /**
     * 메인 실행 메서드 - 트랜잭션을 여기서 시작하지 않고 내부에서 단계별로 호출
     */
    fun syncPopularBoxOffice() {
        // 1. 박스오피스 원천 데이터 수집 (API 호출)
        val rawDataMap = boxOfficeCarryService.syncPopularMovies()
        if (rawDataMap.isEmpty()) return

        // 2. 고유 MovieCd 추출 및 영화 상세 정보 배치 저장 (병렬 처리)
        // 이 단계가 완료되면 DB에 모든 Movie 엔티티가 존재함이 보장됨
        val allMovieCds = rawDataMap.values.flatten().map { it.movieCd }.distinct()
        log.info("--- 2단계: {}건의 영화 상세 정보 병렬 적재 시작 ---", allMovieCds.size)

        allMovieCds.chunked(100).forEach { batch ->
            // BatchService 내부의 @Transactional과 Schedulers에 의해 처리됨
            boxOfficeBatchService.saveMovieDetailBatch(batch)
        }

        // 3. 주차별 박스오피스 순위(BoxOffice 엔티티) 저장 (직렬 트랜잭션)
        log.info("--- 3단계: 주차별 박스오피스 순위 데이터 DB 적재 시작 ---")
        rawDataMap.forEach { (targetDt, weeklyList) ->
            try {
                // 개별 주차별로 트랜잭션을 확실히 분리하여 저장
                this.saveWeeklyBoxOffice(targetDt, weeklyList)
            } catch (e: Exception) {
                log.error("!! [{} 주] 순위 데이터 저장 실패: {}", targetDt, e.message)
            }
        }
    }

    /**
     * 별도의 트랜잭션으로 한 주차의 데이터를 저장
     * saveAll을 사용하여 쿼리 최적화
     */
    @Transactional
    fun saveWeeklyBoxOffice(targetDt: String, weeklyList: List<BoxOfficeInfo>) {
        val entities = weeklyList.mapNotNull { info ->
            // 2단계에서 이미 저장되었으므로 DB에서 조회만 수행
            val movie = koficDataSyncService.saveOrFindMovieData(info.movieCd)

            movie?.let {
                BoxOffice(
                    movie = it,
                    targetDt = targetDt,
                    rank = info.rank,
                    movieNm = info.movieNm,
                    saleAccess = info.salesAcc,
                    audiAcc = info.audiAcc
                )
            }
        }

        if (entities.isNotEmpty()) {
            boxOfficeRepository.saveAll(entities)
            log.info(">> [{}] 주간 박스오피스 {}건 적재 완료", targetDt, entities.size)
        }
    }
}