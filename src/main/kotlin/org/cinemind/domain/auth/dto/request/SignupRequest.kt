package org.cinemind.domain.auth.dto.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

//data class 사용 (Lombok 대체)
data class SignupRequest (

    @Email(message = "이메일 형식에 맞지 않습니다.")
    @NotBlank(message = "이메일은 필수 입력값입니다.")
    val email: String,

    @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다.")
    @NotBlank(message = "비밀번호는 필수 입력값입니다.")
    val password: String
)
