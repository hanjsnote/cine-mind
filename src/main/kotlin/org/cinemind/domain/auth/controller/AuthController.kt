package org.cinemind.domain.auth.controller

import org.cinemind.domain.auth.dto.request.SignupRequest
import org.cinemind.domain.auth.dto.response.SignupResponse
import org.cinemind.domain.auth.service.AuthService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import jakarta.validation.Valid
import org.cinemind.domain.auth.dto.request.SigninRequest
import org.cinemind.domain.auth.dto.response.SigninResponse
import org.springframework.web.bind.annotation.RequestMapping

@RestController
@RequestMapping("/api/auth")
class AuthController (
    private val authService: AuthService
){

    @PostMapping("/signup")
    fun signup(@Valid @RequestBody signupRequest: SignupRequest): SignupResponse {
        return authService.signup(signupRequest)
    }

    @PostMapping("/signin")
    fun signin(@Valid @RequestBody signinRequest: SigninRequest): SigninResponse {
        return authService.signin(signinRequest)
    }

}