package org.cinemind

import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

/**
 * 모든 통합 테스트의 기반이 되는 추상 클래스.
 * 테스트 시작 시 Docker를 사용하여 PostGIS를 포함한 PostgreSQL 컨테이너를 자동으로 띄웁니다.
 * 이 클래스는 PostGIS의 'vector' 타입 지원을 보장합니다.
 */
@Testcontainers
abstract class AbstractIntegrationTest {

    companion object {
        // CRITICAL FIX: postgis 이미지가 postgres 컨테이너와 호환됨을 명시적으로 선언.
        private val POSTGIS_IMAGE: DockerImageName = DockerImageName.parse("ankane/pgvector:latest")
            .asCompatibleSubstituteFor("postgres")

        // PostGIS 확장이 설치된 PostgreSQL 컨테이너를 정의
        @Container
        val postgreSqlContainer: PostgreSQLContainer<*> = PostgreSQLContainer(POSTGIS_IMAGE) // 명시된 이미지 객체 사용
            .withDatabaseName("cinemind_test")
            .withUsername("testuser")
            .withPassword("testpass")
            .withReuse(true)
            // Testcontainers가 컨테이너 시작 시 'vector' 확장 스크립트를 실행하도록 지정
            .withInitScript("init_vector_extension.sql")

        // 런타임에 동적 DB 접속 정보를 Spring 컨텍스트에 주입
        @JvmStatic
        @DynamicPropertySource
        fun setTestPropertySources(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgreSqlContainer::getJdbcUrl)
            registry.add("spring.datasource.username", postgreSqlContainer::getUsername)
            registry.add("spring.datasource.password", postgreSqlContainer::getPassword)
        }
    }
}
