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
import org.cinemind.domain.movie.enums.PeopleRole

// Movie, People 매핑 테이블
@Entity
@Table(name="movie_peoples")
class MoviePeople (

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id")
    val movie: Movie,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "people_id")
    val people: People,

    val role: PeopleRole, // 감독(DIRECTOR), 배우(ACTOR)
    val castNm: String     // 배역명

) : BaseEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
}