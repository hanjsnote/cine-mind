package org.cinemind.domain.auth.service

import jakarta.persistence.Id
import jakarta.security.auth.message.AuthException
import jakarta.transaction.InvalidTransactionException
import jakarta.transaction.Transactional
import org.cinemind.common.exception.CommonErrorCode
import org.cinemind.common.exception.GlobalException
import org.cinemind.config.jwt.JwtUtil
import org.cinemind.domain.auth.dto.request.SigninRequest
import org.cinemind.domain.auth.dto.request.SignupRequest
import org.cinemind.domain.auth.dto.response.SigninResponse
import org.cinemind.domain.auth.dto.response.SignupResponse
import org.cinemind.domain.user.entity.User
import org.cinemind.domain.user.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
@Transactional
class AuthService (
    private val userRepository: UserRepository,
    private val jwtUtil: JwtUtil,
    private val passwordEncoder: PasswordEncoder
) {
    fun signup(signupRequest: SignupRequest): SignupResponse {

        if (userRepository.existsByEmail(signupRequest.email)) {
            throw GlobalException(CommonErrorCode.DUPLICATE_EMAIL)
        }

        val encodedPassword = passwordEncoder.encode(signupRequest.password)

        val user = User(
            email = signupRequest.email,
            password = encodedPassword
        )

        val savedUser = userRepository.save(user)

        val bearerToken: String = jwtUtil.createToken(savedUser.id!!, savedUser.email, savedUser.userRole)

        return SignupResponse(
            bearerToken = bearerToken,
            id = savedUser.id!!,
            email = savedUser.email,
            createdAt = savedUser.createdAt
        )
    }

    fun signin(signinRequest: SigninRequest): SigninResponse {
        val user = userRepository.findByEmail(signinRequest.email)
            ?: throw GlobalException(CommonErrorCode.USER_NOT_FOUND)

        if (!passwordEncoder.matches(signinRequest.password, user.password)) {
            throw GlobalException(CommonErrorCode.INVALID_PASSWORD)
        }

        val bearerToken: String = jwtUtil.createToken(user.id!!, user.email, user.userRole)

        return SigninResponse(bearerToken)
    }
}
