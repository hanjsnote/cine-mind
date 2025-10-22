package org.cinemind.common.exception

import org.springframework.http.HttpStatus

enum class CommonErrorCode(
    override val httpStatus: HttpStatus,
    override val message: String
) : ErrorCode {
    // 400 BadRequest
    INVALID_PARAMETER(HttpStatus.BAD_REQUEST, "요청 파라미터가 유효하지 않습니다."),

    // 401 Unauthorized (인증 실패)
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "잘못된 비밀번호입니다."),

    // 404 Not Found
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "가입되지 않은 유저입니다."),
    MOVIE_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 영화를 찾을 수 없습니다."),

    // 409 Conflict
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 존재하는 이메일입니다."),

    // 500 Internal Server Error (예상치 못한 비즈니스 오류)
    DATABASE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "데이터베이스 처리 중 오류가 발생했습니다.");

    // enum 이름을 ErrorCode의 'code'로 사용
    override val code: String = name
}