package org.cinemind.domain.movie.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.OneToMany
import org.cinemind.common.entity.BaseEntity

// 장르
@Entity
@Table(name="genres")
class Genre (

    val genreNm: String     // 장르명

) : BaseEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    @OneToMany(mappedBy = "genre")
    // MutableSet 순서없는 중복 방지 컬렉션
    val movieGenre: MutableSet<MovieGenre> = mutableSetOf()
}