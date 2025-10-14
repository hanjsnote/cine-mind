package org.cinemind

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class CineMindApplication

fun main(args: Array<String>) {
    runApplication<CineMindApplication>(*args)
}
