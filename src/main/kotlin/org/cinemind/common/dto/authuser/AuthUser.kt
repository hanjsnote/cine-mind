package org.cinemind.common.dto.authuser

import org.cinemind.domain.user.enums.UserRole
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority

// 인증된 사용자 정보를 담는 DTO
class AuthUser (
    val id: Long,
    val email: String,
    val authorities: Collection<GrantedAuthority>
){
    constructor(id: Long, email: String, userRole: UserRole) : this(
        id, email, listOf(SimpleGrantedAuthority(userRole.name))
    )
}