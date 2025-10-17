package org.cinemind.config.jwt

import org.cinemind.common.dto.AuthUser
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority

// JWT로 검증된 사용자 정보(AuthUser)와 권한을 담아 Spring Security가 사용할 수 있는 Authentication 객체를 만드는 클래스
class JwtAuthenticationToken(
    private val authUser: AuthUser,
    // 주 생성자에서 받은 권한 목록(예: ROLE_USER)
    authorities: Collection<GrantedAuthority>
    // 해당 토큰의 권한을 가진 사용자 ROLE_ADMIN/ROLE_USER 판별
) : AbstractAuthenticationToken(authorities){

    init {
        // Security에게 이미 인증된 사용자임을 명시
        isAuthenticated = true
    }

    //JWT 인증에서는 토큰 검증 후 자격 증명이 필요하지 않으므로 null을 반환
    override fun getCredentials(): Any? = null
    //Principal(인증된 사용자)을 반환. (애플리케이션 전체에서 현재 사용자의 정보에 접근하는데 사용)
    override fun getPrincipal(): Any? = authUser
}




