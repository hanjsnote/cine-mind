package org.cinemind.domain.movie.repository

import org.cinemind.domain.movie.entity.Genre
import org.springframework.data.jpa.repository.JpaRepository

interface GenreRepository : JpaRepository<Genre, Long>{
}