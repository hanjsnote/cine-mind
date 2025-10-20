package org.cinemind.domain.auth.service

import jakarta.transaction.InvalidTransactionException
import org.assertj.core.api.Assertions.assertThat
import org.cinemind.domain.auth.dto.request.SignupRequest
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
){
    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder


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

    }

    @Test
    fun 로그인_성공() {

    }
}