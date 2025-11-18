package org.cinemind.domain.chatlog.repository

import org.cinemind.domain.chatlog.entity.ChatLog
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface ChatLogRepository : JpaRepository<ChatLog, Long> {

    // 특정 사용자의 대화 기록을 생성 시간 오름차순으로 조회 (대화 내역 전체 조회용)
    fun findByUserIdOrderByCreatedAtAsc(userId: Long): List<ChatLog>

    // 특정 사용자의 가장 최근 대화 기록을 N개만 조회 (대화 메모리용)
    // 로그인 한 사용자
    fun findByUserIdOrderByCreatedAtDesc(userId: Long, pageable: Pageable): List<ChatLog>
    // 게스트 사용자
    fun findBySessionIdOrderByCreatedAtDesc(sessionId: String, pageable: Pageable): List<ChatLog>
}