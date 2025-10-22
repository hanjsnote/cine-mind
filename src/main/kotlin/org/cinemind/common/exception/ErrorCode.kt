package org.cinemind.common.exception

import org.springframework.http.HttpStatus

interface ErrorCode {
    val code: String
    val httpStatus: HttpStatus
    val message: String
}