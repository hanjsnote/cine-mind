package org.cinemind.domain.movie.entity

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.cinemind.common.entity.BaseEntity

// Movie, Genre 매핑 테이블
@Entity
@Table(name="movie_genres")
class MovieGenre (

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id")
    val movie: Movie,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "genre_id")
    val genre: Genre

) : BaseEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
}