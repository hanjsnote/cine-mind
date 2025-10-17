package org.cinemind.config.jwt

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import jakarta.annotation.PostConstruct
import org.cinemind.domain.user.enums.UserRole
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.util.StringUtils
import java.rmi.ServerException
import java.security.Key
import java.util.Base64
import java.util.Date
import javax.crypto.spec.SecretKeySpec

@Component
class JwtUtil (

    @Value("\${jwt.secret.key}")
    val secretKey: String
){
    companion object {
        const val BEARER_PREFIX = "Bearer "
        const val TOKEN_TIME = 60 * 60 * 1000L * 24
    }
    // JWT 서명에 사용할 비밀키 객체
    private lateinit var key: Key
    private val signatureAlgorithm = SignatureAlgorithm.HS256

    @PostConstruct
    fun init() {
        val bytes = Base64.getDecoder().decode(secretKey)
        key = SecretKeySpec(bytes, signatureAlgorithm.jcaName)
    }

    fun createToken(userId: Long, email: String, userRole: UserRole): String {
        val date = Date()

        return BEARER_PREFIX +
                Jwts.builder()
                    .setSubject(userId.toString())  // userId 고유 식별자
                    .claim("email", email)
                    .claim("userRole", userRole) // enum은 .name으로 문자열 저장
                    .setIssuedAt(date)  // 발급일
                    .setExpiration(Date(date.time + TOKEN_TIME)) // 만료일
                    .signWith(key, signatureAlgorithm) // 서명
                    .compact()
    }

    // "Bearer " 접두어 제거
    fun substringToken(tokenValue: String): String {
        if (StringUtils.hasText(tokenValue) && tokenValue.startsWith(BEARER_PREFIX)){
            return tokenValue.substring(7)
        }
        throw ServerException("Not Found Token")
    }
    // 추출한 JWT 문자열에서 payload(claims)를 파싱하고 서명(Signature)검증 수행
    fun extractClaims(token: String): Claims {
        return Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .body
    }
}