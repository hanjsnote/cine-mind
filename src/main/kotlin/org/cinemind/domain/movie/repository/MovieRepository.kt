package org.cinemind.domain.movie.repository

import org.cinemind.domain.movie.entity.Movie
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
interface MovieRepository : JpaRepository<Movie, Long> {

    @Query("""
        select m.id
        from Movie m
        where not exists (
            select 1
            from MovieEmbedding me
            where me.movie = m
        )
        order by m.id
    """)
    fun findNotIndexedMovieIds(pageable: Pageable): Page<Long>

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
        WHERE m.id IN :ids
    """)
    fun findAllWithRelationsByIdIn(@Param("ids") ids: List<Long>): List<Movie>

    fun findByMovieCd(movieCd: String): Movie?
}