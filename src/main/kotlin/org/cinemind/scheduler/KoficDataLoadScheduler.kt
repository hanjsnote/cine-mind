package org.cinemind.scheduler

import org.cinemind.domain.externalApis.service.BoxOfficeSyncService
import org.cinemind.domain.externalApis.service.KoficDataSyncService
import org.springframework.context.annotation.Profile
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

// 적재 로직 실행 스케줄러
@Component
@Profile("!test")
class KoficDataLoadScheduler (
    private val koficDataSyncService: KoficDataSyncService,
    private val boxOfficeSyncService: BoxOfficeSyncService
) {
    // 영화 목록 적재 스케줄링 (월 1회: 매월 1일 새벽 2시)
    @Scheduled(cron = "0 0 2 1 * ?")
    // 애플리케이션 시작 후 바로 적재 시작
//    @EventListener(ContextRefreshedEvent::class)
    fun initialLoadData() {
        println("Starting scheduled movie list load...")
        koficDataSyncService.saveMovieList()
//        boxOfficeSyncService.startLoadBoxOffice(
//            startDate = "20250101",
//            endDate = "20250101"
//        )
        println("Finished scheduled movie list load.")
    }
}