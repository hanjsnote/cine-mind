package org.cinemind.common.exception

import java.lang.RuntimeException

class GlobalException (
    val errorCode: ErrorCode
) : RuntimeException (
    errorCode.message
)