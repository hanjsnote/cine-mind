package org.cinemind.domain.movie.repository

import org.cinemind.domain.movie.entity.Movie
import org.springframework.data.jpa.repository.JpaRepository

interface MovieRepository : JpaRepository<Movie, Long>{
    fun existsByMovieCd(movieCd: String): Boolean

    fun findByMovieCd(movieCd: String): Movie?
}