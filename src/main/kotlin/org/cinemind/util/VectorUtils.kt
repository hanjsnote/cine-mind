package org.cinemind.util

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * RedisSearch를 위해 FLOAT32 배열(임베딩 벡터)을 Raw Binary Bytes (byte[])로 변환하는 유틸리티
 */
class VectorUtils {

    fun floatsToRawByteArray(floats: FloatArray): ByteArray {
        // 바이트 버퍼 할당 (Float 1은 4바이트)
        val byteBuffer = ByteBuffer.allocate(floats.size * 4)

        // 바이트 순서 지정 (RedisSearch는 Little Endian을 기본으로 사용)
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN)

        // Float 배열을 바이트 버퍼에 기록
        for (f in floats) {
            byteBuffer.putFloat(f)
        }

        // 최종 byte[] 반환
        return byteBuffer.array()
    }
}