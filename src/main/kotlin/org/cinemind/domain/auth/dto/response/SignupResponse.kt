package org.cinemind.domain.auth.dto.response

import java.time.LocalDateTime

data class SignupResponse (

    val id: Long,
    val email: String,
    val createdAt: LocalDateTime?
)