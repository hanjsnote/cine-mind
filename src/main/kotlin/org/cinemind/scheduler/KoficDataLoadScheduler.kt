package org.cinemind.scheduler

import org.cinemind.domain.kofic.service.KoficDataSyncService
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

// 적재 로직 실행 스케줄러
@Component
class KoficDataLoadScheduler (
    private val koficDataSyncService: KoficDataSyncService
) {
    // 영화 목록 적재 스케줄링 (월 1회: 매월 1일 새벽 2시)
//    @Scheduled(cron = "0 0 2 1 * ?")
    // 애플리케이션 시작 후 바로 적재 시작
    @EventListener(ContextRefreshedEvent::class)
    fun scheduleLoadMovieList() {
        println("Starting scheduled movie list load...")
        koficDataSyncService.saveMovieList()
        println("Finished scheduled movie list load.")
    }
}