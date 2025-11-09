package org.cinemind.domain.auth.service

import com.ninjasquad.springmockk.MockkBean
import jakarta.security.auth.message.AuthException
import jakarta.transaction.InvalidTransactionException
import org.assertj.core.api.Assertions.assertThat
import org.cinemind.AbstractIntegrationTest
import org.cinemind.common.exception.CommonErrorCode
import org.cinemind.common.exception.GlobalException
import org.cinemind.domain.auth.dto.request.SigninRequest
import org.cinemind.domain.auth.dto.request.SignupRequest
import org.cinemind.domain.chatbot.controller.ChatController
import org.cinemind.domain.chatbot.service.ChatService
import org.cinemind.domain.user.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class AuthServiceTest @Autowired constructor(
    private val authService: AuthService,
    private val userRepository: UserRepository
) : AbstractIntegrationTest(){
    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @MockkBean
    private lateinit var chatService: ChatService

    @MockkBean
    private lateinit var chatController: ChatController

    @BeforeEach
    fun setup() {
        userRepository.deleteAll()
    }

    @Test
    fun 회원_가입_성공() {
        // given
        val request = SignupRequest("test1@test.com", "12345678")

        // when
        val response = authService.signup(request)

        // then
        assertThat(response.email).isEqualTo("test1@test.com")
        assertThat(response.bearerToken).startsWith("Bearer ")
        assertThat(userRepository.findByEmail("test1@test.com")).isNotNull
    }

    @Test
    fun 이미_존재하는_이메일이면_예외_발생() {
        // given
        val request = SignupRequest("test2@test.com", "12345678")
        authService.signup(request)

        // when & then
        val exception = assertThrows <GlobalException> {
            authService.signup(request)
        }
        assertThat(exception.errorCode).isEqualTo(CommonErrorCode.DUPLICATE_EMAIL)
    }

    @Test
    fun 로그인_성공() {
        // given
        val signup = authService.signup(SignupRequest("test3@test.com", "12345678"))
        val request = SigninRequest("test3@test.com", "12345678")

        // when
        val response = authService.signin(request)

        // then
        assertThat(response.bearerToken).startsWith("Bearer ")
    }

    @Test
    fun 잘못된_비밀번호로_로그인_예외_발생() {
        // given
        val signup = authService.signup(SignupRequest("test4@test.com","12345678"))
        val request = SigninRequest("test4@test.com", "wrongpassword")

        // when & then
        val exception = assertThrows<GlobalException> {
            authService.signin(request)
        }
        assertThat(exception.errorCode).isEqualTo(CommonErrorCode.INVALID_PASSWORD)
    }
}