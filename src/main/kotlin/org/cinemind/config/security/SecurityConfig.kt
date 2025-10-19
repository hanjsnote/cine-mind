package org.cinemind.config.security

import org.cinemind.config.jwt.JwtAuthenticationFilter
import org.cinemind.config.jwt.JwtUtil
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter

@Configuration
@EnableWebSecurity  // Spring Security 활성화
@EnableMethodSecurity(securedEnabled = true)    // @Secured 같은 메서드 단위 권한 제어 활성화
class SecurityConfig (

    private val jwtUtil: JwtUtil,
    private val jwtAuthenticationFilter: JwtAuthenticationFilter
){
    @Bean
    fun passwordEncoder(): PasswordEncoder{
        return BCryptPasswordEncoder()
    }

    @Bean
    @Throws(Exception::class)
    fun securityFilterChain(httpSecurity: HttpSecurity) : SecurityFilterChain {
        return httpSecurity
            // CSRF 비활성화 (JWT 사용 시 불필요)
            .csrf{it.disable()}
            // 세션 관리 : STATELESS 설정
            .sessionManagement{ it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            // 커스텀 JWT 필터 등록
            .addFilterBefore(jwtAuthenticationFilter, BasicAuthenticationFilter::class.java)

            // 요청별 권한 제어
            .authorizeHttpRequests { auth ->
                auth
                    // /auth로 시작하는 모든 요청 허용 (회원가입, 로그인 등)
                    .requestMatchers("/api/auth/**").permitAll()
                    // 기토 공개 엔드포인트
                    .requestMatchers("/open", "/health").permitAll()
                    // 나머지 모든 요청은 인증 필요
                    .anyRequest().authenticated()
            }
            .build()
    }
}