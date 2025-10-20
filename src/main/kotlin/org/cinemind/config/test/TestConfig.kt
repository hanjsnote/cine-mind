package org.cinemind.config.test

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.data.domain.AuditorAware
import java.util.Optional

@Configuration
@Profile("test")
class TestConfig {
    @Bean
    fun disableJpaAuditing(): AuditorAware<String> = AuditorAware { Optional.empty() }
}