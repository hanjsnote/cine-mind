package org.cinemind.config.Admin

import jakarta.annotation.PostConstruct
import jakarta.transaction.Transactional
import org.cinemind.domain.user.entity.User
import org.cinemind.domain.user.enums.UserRole
import org.cinemind.domain.user.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

/**
 * 애플리케이션 시작 시 관리자 계정이 없는 경우, 환경 변수 기반으로 관리자 계정을 생성.
 */
@Component
class AdminInitializer (
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,

    // 이메일은 환경 변수 `ADMIN_EMAIL`에서 주입
    @Value("\${ADMIN_EMAIL}")
    private val adminEmail: String,

    // 비밀번호는 환경 변수 `ADMIN_PASSWORD`에서 주입
    @Value("\${ADMIN_PASSWORD")
    private val adminPassword: String
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PostConstruct
    @Transactional
    fun initializeAdminUser() {
        // 관리자 이메일을 가진 사용자가 이미 존재하는지 확인
        if (userRepository.existsByEmail(adminEmail)) {
            log.info("[Admin Init] 관리자 계정 ({})이 이미 존재합니다. 초기화를 건너뜁니다.", adminEmail)
            return
        }

        // 관리자 계정 생성
        val encodedPassword = passwordEncoder.encode(adminPassword)

        // User 엔티티의 userRole을 지정하는 보조 생성자 사용
        val adminUser = User(
            email = adminEmail,
            password = encodedPassword,
            userRole = UserRole.ROLE_ADMIN  // 명시적으로 ADMIN 역할 부여
        )

        userRepository.save(adminUser)
        log.warn("================================================================")
        log.warn("!!! [Admin Init] 관리자 계정이 생성되었습니다. !!!")
        log.warn("!!! EMAIL: {} / PASSWORD: {} !!!", adminEmail, adminPassword)
        log.warn("================================================================")
    }
}