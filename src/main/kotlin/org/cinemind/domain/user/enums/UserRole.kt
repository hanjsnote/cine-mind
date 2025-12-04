package org.cinemind.domain.user.enums

import org.cinemind.common.dto.authuser.AuthUser
import java.util.Arrays

enum class UserRole(val userRole: String){
    ROLE_USER(Authority.USER),
    ROLE_ADMIN(Authority.ADMIN);

    //companion object는 자바의 static 메서드/필드 역할을 한다. of()를 static 처럼 사용함
    companion object {
        fun of(role: String): UserRole {
            return entries.firstOrNull{
                it.name.equals(role, ignoreCase = true)
            }
                ?: throw IllegalArgumentException("유효하지 않은 UserRole: $role")
        }
    }
    object Authority {
        const val USER = "ROLE_USER"
        const val ADMIN = "ROLE_ADMIN"
    }
}

