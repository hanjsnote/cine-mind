package org.cinemind.scheduler

import org.cinemind.domain.externalApis.service.BoxOfficeSyncService
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
/**
 * 정기적으로 박스오피스 및 인기 영화 데이터를 동기화하는 스케줄러.
 */
@Component
@Profile("!test")
class BoxOfficeBatchScheduler (
    private val boxOfficeSyncService: BoxOfficeSyncService
) {
    private val log = LoggerFactory.getLogger(BoxOfficeBatchScheduler::class.java)

    // 주말 박스오피스 적재 스케줄링 (월 1회: 매월 1일 새벽 4시)
    @Scheduled(cron = "0 0 4 1 * ?")
//    @EventListener(ContextRefreshedEvent::class)
    fun runBoxOfficeScheduler() {
        log.info("=========================================================")
        log.info("  [스케줄러 시작] 주말 박스오피스 기반 인기 영화 데이터 동기화 시작.")
        log.info("=========================================================")

        try {
            boxOfficeSyncService.syncPopularBoxOffice()
            log.info("=========================================================")
            log.info("  [스케줄러 종료] 인기 영화 데이터 동기화 완료.")
            log.info("=========================================================")
        } catch (e: Exception) {
            log.error("!! [스케줄러 오류] 인기 영화 데이터 동기화 중 치명적인 오류 발생: {}", e.message, e)
        }
    }
}