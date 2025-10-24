package org.cinemind.domain.movie.repository

import org.cinemind.domain.movie.entity.Genre
import org.cinemind.domain.movie.entity.MovieGenre
import org.springframework.data.jpa.repository.JpaRepository

interface MovieGenreRepository : JpaRepository<MovieGenre, Long>{

}