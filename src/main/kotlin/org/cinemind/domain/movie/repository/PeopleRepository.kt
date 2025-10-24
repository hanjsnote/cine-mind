package org.cinemind.domain.movie.repository

import org.cinemind.domain.kofic.dto.response.ActorKofic
import org.cinemind.domain.kofic.dto.response.GenreKofic
import org.cinemind.domain.movie.entity.People
import org.springframework.data.jpa.repository.JpaRepository

interface PeopleRepository : JpaRepository<People, Long> {
    fun findByPeopleNm(peopleDto: String): People?
}