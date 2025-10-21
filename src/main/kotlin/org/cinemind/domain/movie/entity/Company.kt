package org.cinemind.domain.movie.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.cinemind.common.entity.BaseEntity

// 제작사 정보
@Entity
@Table(name="companys")
class Company (



) : BaseEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
}