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
) : CommandLineRunner {
    // 임베딩 스케줄링 (월 1회: 매월 1시 새벽 3시)
//    @Scheduled(cron = "0 0 3 1 * ?")

    override fun run(vararg args: String?) {
        println("--- [RAG] 영화 데이터 인덱싱 작업을 시작합니다 ---")
        try {
            ragIndexingService.indexAllMovies()
            println("--- [RAG] 영화 데이터 인덱싱 작업 완료 ---")
        } catch (e: Exception) {
            println("--- [RAG] 영화 데이터 인덱싱 중 오류 발생: ${e.message}")
        }
    }
}