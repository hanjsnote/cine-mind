package org.cinemind.config.security

import org.cinemind.config.jwt.JwtAuthenticationFilter
import org.cinemind.config.jwt.JwtUtil
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
                    // 프론트엔드/정적 파일 접근 허용
                    // 브라우저에서 http://3.39.99.136:8080/ 접속 시 필요.
                    .requestMatchers(
                        "/",   // 루트 경로 (액세스 거부 문제 해결)
                        "/index.html",    // HTML 파일
                        "/css/**",        // CSS 파일
                        "/js/**",         // JavaScript 파일
                        "/images/**",     // 이미지 파일
                        "/error"          // Spring 기본 에러 페이지
                    ).permitAll()

                    // 공개 API 엔드포인트 허용
                    .requestMatchers("/api/auth/**").permitAll() // 회원가입, 로그인
                    .requestMatchers("/open", "/api/health", "/api/chat").permitAll() // 헬스 체크, 채팅(비로그인 가능 시)

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
        config.allowedOrigins = listOf("http://localhost:5173")
        config.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
        config.allowedHeaders = listOf("*")
        config.allowCredentials = true

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", config)
        return source
    }
}