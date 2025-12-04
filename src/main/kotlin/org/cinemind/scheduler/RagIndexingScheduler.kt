package org.cinemind.scheduler

import org.cinemind.domain.rag.service.RagIndexingService
import org.springframework.boot.CommandLineRunner
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
    // 테스트를 위해 시작 후 1분 뒤 실행 (cron 표현식: "초 분 시 일 월 요일")
    // 현재: 매분 0초에 실행되도록 임시 변경 (테스트 용도)
    @Scheduled(cron = "0 * * * * ?") // 매분 0초마다 실행
    // 원래 배포용 임베딩 스케줄링: 매월 1일 새벽 3시에 실행 (신규 영화만 인덱싱)
    // @Scheduled(cron = "0 0 3 1 * ?")
    fun scheduledIncrementalIndexing() {
        println("--- [RAG] 월별 신규 영화 데이터 인덱싱 작업을 시작합니다 ---")
        try {
            // 새로 추가된 영화 데이터만 증분 인덱싱
            ragIndexingService.indexNewMovies()
            println("--- [RAG] 월별 신규 영화 데이터 인덱싱 작업 완료 ---")
        } catch (e: Exception) {
            println("--- [RAG] 신규 영화 데이터 인덱싱 중 오류 발생: ${e.message}")
        }
    }
}