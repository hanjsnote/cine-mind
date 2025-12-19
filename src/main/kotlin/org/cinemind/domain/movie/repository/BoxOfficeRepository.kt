package org.cinemind.domain.movie.repository

import org.cinemind.domain.movie.entity.BoxOffice
import org.cinemind.domain.movie.entity.Movie
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param


interface BoxOfficeRepository : JpaRepository<BoxOffice, Long>{
    // 특정 주차(targetDt)의 데이터가 하나라도 있는지 확인
    fun existsByTargetDt(targetDt: String): Boolean
    // 특정 영화의 특정 날짜 통계 중복 확인 로직
    fun existsByMovieAndTargetDt(movie: Movie, target: String): Boolean

    fun countByTargetDt(targetDt: String): Long

    @Query("""
        select distinct b.targetDt
        from BoxOffice b
        where b.targetDt between :startDt and :endDt
    """)
    fun findExistingTargetDtsBetween(
        @Param("startDt") startDt: String,
        @Param("endDt") endDt: String
    ): Set<String>
}