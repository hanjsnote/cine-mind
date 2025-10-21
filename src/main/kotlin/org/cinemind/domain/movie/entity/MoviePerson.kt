package org.cinemind.domain.movie.entity

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.apache.catalina.Role
import org.cinemind.common.entity.BaseEntity
import org.cinemind.domain.movie.enums.PersonRole

// Movie, Person 매핑 테이블
// 배우(actor), 감독(director)은 role로 구분, cast는 배역(역할)
@Entity
@Table(name="movie_persons")
class MoviePerson (

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id")
    val movie: Movie,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id")
    val person: Person,

    val role: PersonRole, // 감독(DIRECTOR), 배우(ACTOR)
    val castNm: String     // 배역(역할)

) : BaseEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
}