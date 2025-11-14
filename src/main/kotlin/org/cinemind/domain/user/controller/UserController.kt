package org.cinemind.domain.user.controller

import org.cinemind.common.dto.authuser.AuthUser
import org.cinemind.domain.user.dto.response.FindAllResponse
import org.cinemind.domain.user.service.UserService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/users")
class UserController (
    private val userService: UserService
){
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    fun findAll(
        @AuthenticationPrincipal authUser: AuthUser,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ) : Page<FindAllResponse?> {
        val pageable: Pageable = PageRequest.of(page, size)
        return userService.findAll(authUser, pageable)
    }
}