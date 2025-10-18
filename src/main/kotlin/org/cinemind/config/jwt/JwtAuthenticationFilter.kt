package org.cinemind.config.jwt

import com.fasterxml.jackson.databind.ObjectMapper
import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.UnsupportedJwtException
import io.jsonwebtoken.io.IOException
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.lang.NonNull
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
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
        //Bearer 접두어 제거
        val jwt = jwtUtil.substringToken(authorizationHeader)

        //JWT 검증 및 인증 설정 실패하면 에러 응답(JSON), 성공하면 processAuthentication에서 Claims 추출 후 SecurityContext에 사용자 정보 세팅
        if (!processAuthentication(jwt, httpRequest, httpResponse)) {
            return
        }

        // JWT 검증 성공 시 다음 필터로 요청 전달
        chain.doFilter(httpRequest, httpResponse)
    }

    //JWT 토큰을 검증하고 SecurityContext에 인증 정보를 설정하는 메서드
    @Throws(IOException::class)
    private fun processAuthentication(
        jwt: String,
        httpRequest: HttpServletRequest,
        httpResponse: HttpServletResponse
    ) : Boolean {
        try {
            //JWT 토큰을 파싱하여 Claims(토큰에 담긴 정보) 추출
            val claims: Claims = jwtUtil.extractClaims(jwt)

            //SecurityContext 인증 정보가 없으면 설정(이미 인증된 경우 중복 설정 방지)
            if (SecurityContextHolder.getContext().authentication == null){
                setAuthentication(claims)
            }
            return true     //검증 성공
        } catch (e: Exception) {
            when (e) {
                is ExpiredJwtException -> {
                    log.info("JWT expired: userId={}, URI={}", e.claims.subject, httpRequest.requestURI)
                    sendErrorResponse(httpResponse, HttpStatus.UNAUTHORIZED, "인증이 필요합니다. (토큰 만료)")
                }
                // 쉼표(,)를 사용하여 여러 예외를 묶어 처리
                is SecurityException, is MalformedJwtException, is UnsupportedJwtException -> {
                    log.error("JWT 검증 실패 [{}]: URI={}", e::class.simpleName, httpRequest.requestURI, e)
                    sendErrorResponse(httpResponse, HttpStatus.BAD_REQUEST, "인증이 필요합니다.")
                }
                else -> {
                    log.error("Unexpected error: URI={}", httpRequest.requestURI, e)
                    sendErrorResponse(httpResponse, HttpStatus.INTERNAL_SERVER_ERROR, "요청 처리 중 오류가 발생했습니다.")
                }
            }
            return false    //검증 실패
        }
    }

    //JWT Claims에서 사용자 정보를 추출하여 Spring Security의 인증 정보 설정
    private fun setAuthentication(claims: Claims){

    }

    @Throws(IOException::class)
    private fun sendErrorResponse(httpResponse: HttpServletResponse, status: HttpStatus, message: String){

    }

}