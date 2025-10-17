package org.cinemind.domain.auth.service

import jakarta.transaction.InvalidTransactionException
import jakarta.transaction.Transactional
import org.cinemind.domain.auth.dto.request.SignupRequest
import org.cinemind.domain.auth.dto.response.SignupResponse
import org.cinemind.domain.user.entity.User
import org.cinemind.domain.user.repository.UserRepository
import org.springframework.stereotype.Service

@Service
@Transactional
class AuthService (
    private val userRepository: UserRepository
){
    fun signup(signupRequest: SignupRequest): SignupResponse {

        if (userRepository.existsByEmail(signupRequest.email)) {
            throw InvalidTransactionException("이미 존재하는 이메일 입니다.")
        }

        val user = User(
            email = signupRequest.email,
            password = signupRequest.password
        )
        val savedUser = userRepository.save(user)

        return SignupResponse(
            id = savedUser.id!!,
            email = savedUser.email,
            createdAt = savedUser.createdAt
        )
    }
}