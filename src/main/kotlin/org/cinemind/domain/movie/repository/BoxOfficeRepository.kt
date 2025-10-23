package org.cinemind.domain.movie.repository

import org.cinemind.domain.movie.entity.BoxOffice
import org.cinemind.domain.movie.entity.Movie
import org.springframework.data.jpa.repository.JpaRepository


interface BoxOfficeRepository : JpaRepository<BoxOffice, Long>{
    // 특정 영화의 특정 날짜 통계 중복 확인 로직
    fun existsByMovieAndTargetDt(movie: Movie, target: String): Boolean
}