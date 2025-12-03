package org.cinemind.domain.health

import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.sql.DataSource

@RestController
@RequestMapping("/api/health")
class HealthController(
    private val dataSource: DataSource,
    private val redisConnectionFactory: RedisConnectionFactory
) {

    @GetMapping
    fun health(): Map<String, Any> {
        val details = mutableMapOf<String, Any>()

        // 1) DB 생존 여부 체크
        val dbStatus = runCatching {
            dataSource.connection.use { conn ->
                conn.prepareStatement("SELECT 1").execute()
            }
        }.fold(
            onSuccess = { "UP" },
            onFailure = { ex ->
                details["dbError"] = ex.message ?: "unknown"
                "DOWN"
            }
        )

        // 2) Redis 생존 여부 체크
        val redisStatus = runCatching {
            redisConnectionFactory.connection.use { conn ->
                // 아무 명령이나 하나만 수행해도 됨. 여기선 TTL 조회 정도로 가볍게.
                conn.keyCommands().exists("health-check".toByteArray())
            }
        }.fold(
            onSuccess = { "UP" },
            onFailure = { ex ->
                details["redisError"] = ex.message ?: "unknown"
                "DOWN"
            }
        )

        // 3) 전체 상태
        val overallUp = dbStatus == "UP" && redisStatus == "UP"

        return mapOf(
            "status" to if (overallUp) "UP" else "DOWN",
            "db" to dbStatus,
            "redis" to redisStatus,
            "details" to details
        )
    }
}