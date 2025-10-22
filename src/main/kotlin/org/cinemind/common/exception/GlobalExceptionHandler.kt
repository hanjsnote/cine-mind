package org.cinemind.common.exception

import jakarta.servlet.http.HttpServletRequest
import org.cinemind.common.dto.response.ApiErrorResponse
import org.cinemind.config.jwt.JwtAuthenticationFilter
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    companion object {
        private val log = LoggerFactory.getLogger(JwtAuthenticationFilter::class.java)
    }

    // 비즈니스 예외
    @ExceptionHandler(GlobalException::class)
    fun handleGlobalException(ex: GlobalException, req: HttpServletRequest) : ResponseEntity<ApiErrorResponse> {

        log.error("Business error: code={}, uri={}, method={}, msg={}", ex.errorCode.code, req.requestURI, req.method, ex.message)
        return response(ex.errorCode)
    }

    // 서버 오류 시 (최종)
    @ExceptionHandler(Exception::class)
    fun handleException(ex: Exception, req: HttpServletRequest) : ResponseEntity<ApiErrorResponse> {

        log.error("Unhandled error: uri={}, method={}, msg={}", req.requestURI, req.method, ex.message, ex)
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.")
    }

    // 공통 ResponseEntity 빌더 ErrorCode 사용
    private fun response(errorCode: ErrorCode) : ResponseEntity<ApiErrorResponse> {
        return ResponseEntity
            .status(errorCode.httpStatus)
            .body(ApiErrorResponse.from(errorCode))
    }

    // 공통 ResponseEntity 빌더 HttpStatus와 Message 사용
    private fun response(httpStatus: HttpStatus, message: String) : ResponseEntity<ApiErrorResponse> {
        return ResponseEntity
            .status(httpStatus)
            .body(ApiErrorResponse.from(httpStatus, message))
    }
}