package org.cinemind.config.jwt

import com.fasterxml.jackson.databind.ObjectMapper
import io.jsonwebtoken.io.IOException
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.lang.NonNull
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import kotlin.jvm.Throws

// JWT 검증 및 Security Context에 인증 정보를 설정하는 필터
@Component
class JwtAuthenticationFilter (
    private val jwtUtil: JwtUtil,
    private val objectMapper: ObjectMapper
) : OncePerRequestFilter() {

    companion object {
        private val log = LoggerFactory.getLogger(JwtAuthenticationFilter::class.java)
    }
    @Throws(ServletException::class, IOException::class)
    override fun doFilterInternal(
        httpRequest: HttpServletRequest,
        httpResponse: HttpServletResponse,
        chain: FilterChain
    ) {
        val authorizationHeader = httpRequest.getHeader("Authorization")

        log.info("JwtAuthenticationFilter - request URI: {}", httpRequest.requestURI)

        //Authorization 헤더가 없거나 "Bearer "로 시작하지 않으면 JWT 인증을 건너뜀
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")){
            chain.doFilter(httpRequest, httpResponse)
            return
        }

        val jwt = jwtUtil.substringToken(authorizationHeader)

        //JWT 검증 및 인증 설정 실패하면 에러 응답(JSON), 성공하면 processAuthentication에서 Claims 추출 후 SecurityContext에 사용자 정보 세팅
//        if (!processAuthentication(jwt, httpRequest, httpResponse)) {
//            return
//        }

        // JWT 검증 성공 시 다음 필터로 요청 전달
        chain.doFilter(httpRequest, httpResponse)
    }

}