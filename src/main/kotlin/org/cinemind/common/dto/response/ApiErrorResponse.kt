package org.cinemind.common.dto.response

import org.cinemind.common.exception.ErrorCode
import org.springframework.http.HttpStatus
import java.time.LocalDateTime

class ApiErrorResponse (
    val success: Boolean,
    val message: String,
    val data: Any?,
    val timestamp: LocalDateTime
){
    companion object {
        fun from(httpStatus: HttpStatus, message: String) : ApiErrorResponse {
            return ApiErrorResponse(false, message, null, LocalDateTime.now())
        }

        fun from(errorCode: ErrorCode) : ApiErrorResponse {
            return ApiErrorResponse(false, errorCode.message, null, LocalDateTime.now())
        }
    }
}