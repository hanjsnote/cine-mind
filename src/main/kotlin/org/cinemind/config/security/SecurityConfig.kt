package org.cinemind.config.security

import org.cinemind.config.jwt.JwtAuthenticationFilter
import org.cinemind.config.jwt.JwtUtil
import org.cinemind.domain.user.enums.UserRole
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

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
            // CORS 활성화
            .cors { }
            // 세션 관리 : STATELESS 설정
            .sessionManagement{ it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            // 커스텀 JWT 필터 등록
            .addFilterBefore(jwtAuthenticationFilter, BasicAuthenticationFilter::class.java)

            // 요청별 권한 제어
            .authorizeHttpRequests { auth ->
                auth
                    // /auth로 시작하는 모든 요청 허용 (회원가입, 로그인 등)
                    .requestMatchers("/api/auth/**").permitAll()
                    // 관리자(admin) 인덱싱 엔드포인트
                    .requestMatchers("/api/rebuild-all", "/api/incremental").hasAuthority(UserRole.Authority.ADMIN)
                    // 기타 공개 엔드포인트
                    .requestMatchers("/open", "/api/health", "/api/chat").permitAll()
                    // CORS 프리플라이트 옵션 허용
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    // 나머지 모든 요청은 인증 필요
                    .anyRequest().authenticated()
            }
            .build()
    }
    // CORS 전역설정
    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val config = CorsConfiguration()
        config.allowedOrigins = listOf(
            "http://localhost:5173", // 기존 로컬 개발 환경
            "https://cinemind.me", // 프로덕션 주소 1
            "https://www.cinemind.me" // 프로덕션 주소 2
        )
        config.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
        config.allowedHeaders = listOf("*")
        config.allowCredentials = true

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", config)
        return source
    }
}