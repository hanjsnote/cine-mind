package org.cinemind.domain.user.repository

import org.cinemind.domain.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository: JpaRepository<User, Long> {

    fun existsByEmail(email: String): Boolean
    fun findByEmail(email: String): User?
}


