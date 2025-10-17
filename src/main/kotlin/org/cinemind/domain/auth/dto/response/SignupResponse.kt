package org.cinemind.domain.auth.dto.response

import java.time.LocalDateTime

data class SignupResponse (

    val bearerToken: String,
    val id: Long,
    val email: String,
    val createdAt: LocalDateTime?
)