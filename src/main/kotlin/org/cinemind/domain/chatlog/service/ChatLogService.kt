package org.cinemind.domain.chatlog.service

import org.springframework.stereotype.Service

@Service
class ChatLogService {
    fun getLogsByUserId(id: Long) {

        return
    }

    fun saveUserMessage(id: Long, userQuery: String) {}
    fun chatAssistantMessage(userId: Long, content: String, queryKeywords: List<String>, relatedMovieCodes: List<String>) {}

}