package org.cinemind.domain.chatlog.repository

import org.cinemind.domain.chatlog.entity.ChatLog
import org.springframework.data.jpa.repository.JpaRepository

interface ChatLogRepository : JpaRepository<ChatLog, Long> {
}