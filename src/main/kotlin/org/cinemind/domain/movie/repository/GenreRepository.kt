package org.cinemind.domain.movie.repository

import org.cinemind.domain.movie.entity.Genre
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface GenreRepository : JpaRepository<Genre, Long> {

    fun findByGenreNm(genreNm: String): Genre?
    fun findAllByGenreNmIn(genreNames: List<String>): List<Genre>

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        value = """
            INSERT INTO genres (genre_nm, created_at, modified_at)
            SELECT x, now(), now()
            FROM unnest(CAST(:names AS text[])) AS x
            ON CONFLICT (genre_nm) DO NOTHING
        """,
        nativeQuery = true
    )
    fun upsertIgnoreAll(@Param("names") names: Array<String>): Int
}