package org.cinemind.domain.movie.repository

import org.cinemind.domain.movie.entity.Movie
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface MovieRepository : JpaRepository<Movie, Long>{

    // RAG 인덱싱을 위해 모든 연관 엔티티를 Fetch join하여 N+1 문제 방지
    @Query("""
        SELECT DISTINCT m 
        FROM Movie m
        LEFT JOIN FETCH m.movieGenre mg
        LEFT JOIN FETCH mg.genre
        LEFT JOIN FETCH m.moviePeople mp
        LEFT JOIN FETCH mp.people
        LEFT JOIN FETCH m.movieCompany mc
        LEFT JOIN FETCH mc.company
        LEFT JOIN FETCH m.boxOffice bo
    """)
    override fun findAll (): List<Movie>

    // KoficDataSyncService의 saveOrFindMovieData 함수에서 사용: 영화 코드로 Movie 엔티티를 조회
    fun findByMovieCd(movieCd: String): Movie?

}