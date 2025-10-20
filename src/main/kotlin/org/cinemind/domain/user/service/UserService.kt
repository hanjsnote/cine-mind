package org.cinemind.domain.user.service

import org.cinemind.common.dto.AuthUser
import org.cinemind.domain.user.dto.response.FindAllResponse
import org.cinemind.domain.user.repository.UserRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class UserService (
    private val userRepository: UserRepository
){
    fun findAll(authUser: AuthUser, pageable: Pageable): Page<FindAllResponse?> {
        val users = userRepository.findAll(pageable)
        return users.map { user ->
            FindAllResponse(
                id = user.id!!,
                email = user.email
            )
        }
    }
}