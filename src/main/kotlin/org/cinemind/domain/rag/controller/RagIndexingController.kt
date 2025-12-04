package org.cinemind.domain.rag.controller

import org.cinemind.common.dto.authuser.AuthUser
import org.cinemind.domain.rag.service.RagIndexingService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class RagIndexingController (
    private val ragIndexingService: RagIndexingService
) {
    // 관리자 권한으로 기존 엔덱싱 데이터를 삭제하고 새로 생성
    @PostMapping("/rebuild-all")
    fun rebuildAllIndexing(@AuthenticationPrincipal authUser: AuthUser): ResponseEntity<String> {
        val indexedCount = ragIndexingService.rebuildAllIndexes(authUser)

        return ResponseEntity.ok(
            "관리자(${authUser.id})에 의해 전체 RAG 인덱스 재구축 완료. " +
                    "총 ${indexedCount}개의 영화가 인덱싱되었습니다."
        )
    }

    // 관리자 권한으로 신규 영화만 증분 인덱싱
    @PostMapping("/incremental")
    fun indexNewMovies(@AuthenticationPrincipal authUser: AuthUser): ResponseEntity<String>{
        val indexedCount = ragIndexingService.indexNewMovies(authUser)

        val message = if (indexedCount > 0) {
            "관리자(${authUser.id})에 의해 신규 영화 RAG 인덱스 증분 업데이트 완료. " +
                    "총 ${indexedCount}개의 영화가 추가되었습니다."
        } else {
            "관리자(${authUser.id}): 새로 인덱싱할 영화 데이터가 없습니다."
        }

        return ResponseEntity.ok(message)
    }
}