package org.cinemind

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import javax.sql.DataSource

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = [
    // 💡 PostgreSQL 설정을 H2 설정으로 강제 오버라이드
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa",
    "spring.datasource.password=",

    // 💡 JWT 설정도 함께 오버라이드 (이전 @ActiveProfiles("test")의 역할)
    "jwt.secret.key=abcdabcdabcdabcdabcdabcdabcdabcdabcdabcdabcdabcdabcd",
    "kofic.api.key=test-kofic-api-key-test-kofic-api-key-test-kofic-api-key"
])

//@SpringBootTest
class CineMindApplicationTests {

    @Test
    fun contextLoads() {
    }

}
