package org.cinemind.domain.movie.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.cinemind.common.entity.BaseEntity

// 영화 기본 정보
@Entity
@Table(name="movies")
class Movie (



) : BaseEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
}