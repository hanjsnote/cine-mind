package org.cinemind.util

import org.cinemind.domain.chatlog.entity.ChatLog
/**
 * ChatLog 에 저장된 queryKeywords("모아나 2, 마우이, 애니메이션")를
 * List<String> 으로 변환하는 확장 함수.
 */
fun ChatLog.keywordList(): List<String> {
    return this.queryKeywords
        ?.split(",")
        ?.map { it.trim() }
        ?.filter { it.isNotBlank() }
        ?: emptyList()
}
