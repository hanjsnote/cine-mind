package org.cinemind

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@EnableJpaAuditing
@SpringBootApplication
class CineMindApplication

fun main(args: Array<String>) {
    runApplication<CineMindApplication>(*args)
}
