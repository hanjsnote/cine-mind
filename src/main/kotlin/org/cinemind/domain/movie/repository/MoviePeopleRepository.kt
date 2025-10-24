package org.cinemind.domain.movie.repository

import org.cinemind.domain.movie.entity.MoviePeople
import org.cinemind.domain.movie.entity.People
import org.springframework.data.jpa.repository.JpaRepository

interface MoviePeopleRepository : JpaRepository<MoviePeople, Long> {
}