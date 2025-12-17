package org.cinemind.domain.externalApis.service

import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

/**
 * KOFIC 주말 박스오피스를 병렬 처리로 한번에 동시에 호출하는 클래스
 */
@Service
class BoxOfficeBatchService (
    private val koficDataSyncService: KoficDataSyncService
) {
    private val log = LoggerFactory.getLogger(BoxOfficeBatchService::class.java)
    private val PARALLEL_DEGREE = 5 // 최대 병렬 API 호출 수

    // 100건의 MovieCd 목록을 받아 처리하는 트랜잭션 단위
    @Transactional
    fun saveMovieDetailBatch(movieCds: List<String>) {
        log.info("   [배치 실행 ] {}건의 MovieCd에 대해 상세 정보 비동기 호출 및 저장 시작. 병렬도: {}", movieCds.size, PARALLEL_DEGREE)

        // Flux.formIterable: MovieCd 목록을 스트림으로 만든다
        // flatMap(PARALLEL_DEGREE): N개의 생세 정보 API 호출을 동시에 병렬 처리
        Flux.fromIterable(movieCds)
            .flatMap({ movieCd: String ->
                Mono.fromCallable {
                    koficDataSyncService.saveOrFindMovieData(movieCd)
                }.subscribeOn(Schedulers.boundedElastic()) // 블로킹 호출을 처리할 스케줄러 지정
            }, PARALLEL_DEGREE)
            .collectList() // 모든 처리가 완료될 때까지 기다림
            .block() // 전체 배치 작업이 완료될 때까지 Blocking
        log.info("  [배치 종료] {}건 중 병렬 처리 완료.", movieCds.size)
    }
}