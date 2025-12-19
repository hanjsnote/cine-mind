package org.cinemind.scheduler

import org.cinemind.domain.rag.service.RagIndexingService
import org.slf4j.LoggerFactory
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.core.annotation.Order
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * String Boot 애플리케이션 시작 시점에 RagIndexingService의 indexAllMovies()를 실행하는 컴포넌트
 */
@Component
@Order(2)   // KOFIC 데이터 로딩 후 실행되도록 순서 지정
class RagIndexingScheduler (
    private  val ragIndexingService: RagIndexingService
) {
    private val log = LoggerFactory.getLogger(javaClass)
    // 매월 1일 새벽 3시에 실행 (신규 영화만 인덱싱)
//     @Scheduled(cron = "0 0 3 1 * ?")
    @EventListener(ContextRefreshedEvent::class)
    fun scheduledIncrementalIndexing() {
        println("--- [RAG] 월별 신규 영화 데이터 인덱싱 작업을 시작합니다 ---")
        try {
            // 새로 추가된 영화 데이터만 증분 인덱싱
            val indexedCount = ragIndexingService.indexNewMovies(null)
            log.info("--- [RAG Scheduler] 월별 신규 영화 데이터 인덱싱 작업 완료. (총 {}개 인덱싱) ---", indexedCount)
        } catch (e: Exception) {
            log.error("--- [RAG Scheduler] 신규 영화 데이터 인덱싱 중 오류 발생: {}", e.message)
        }
    }
}