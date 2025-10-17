package org.cinemind.config.jwt

import io.jsonwebtoken.SignatureAlgorithm
import org.springframework.beans.factory.annotation.Value
import java.security.Key

class JwtUtil (

    @Value("\${jwt.secret.key}")
    val secretKey: String
){
    companion object {
        const val BEARER_PREFIX = "Bearer "
        const val TOKEN_TIME = 60 * 60 * 1000L * 24
    }

    private lateinit var key: Key
    private val signatureAlgorithm = SignatureAlgorithm.HS256


}