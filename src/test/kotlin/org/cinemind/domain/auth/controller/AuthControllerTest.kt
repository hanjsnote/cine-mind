package org.cinemind.domain.auth.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.cinemind.domain.auth.service.AuthService
import org.cinemind.config.jwt.JwtUtil
import org.cinemind.domain.auth.dto.request.SignupRequest
import org.cinemind.domain.auth.dto.response.SignupResponse
import org.junit.jupiter.api.Disabled
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration

import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles

import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime
import kotlin.test.Test

@Disabled
@WebMvcTest(
    controllers = [AuthController::class],
    // 💡 JPA 및 데이터소스 자동 구성을 명시적으로 제외
    excludeAutoConfiguration = [
        DataSourceAutoConfiguration::class,
        HibernateJpaAutoConfiguration::class,

    ]
)
@Import(JwtUtil::class)
@ActiveProfiles("test")
class AuthControllerTest @Autowired constructor(

    private val mockMvc: MockMvc,
){
    // AuthService를 가짜 객체로 대체
    @MockkBean
    private lateinit var authService: AuthService

    @Test
    fun 회원_가입_요청_성공(){
        // given
        val request = SignupRequest("user@test.com", "12345678")
        val response = SignupResponse("Bearer token", 1L, "user@test.com", LocalDateTime.now())

        // AuthService를 실제로 호출하지 않고 무조건 성공적인 응답을 돌려주게 함 DB접근 방지
        every {
            authService.signup(request)
        } returns (response)

        // when
        mockMvc.perform(
            post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"user@test.com","password":"12345678"}""")
        )
            // then
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.email").value("user@test.com"))
            .andExpect(jsonPath("$.bearerToken").value("Bearer token"))
    }
}