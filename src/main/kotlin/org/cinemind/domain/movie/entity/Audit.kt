package org.cinemind.domain.movie.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.cinemind.common.entity.BaseEntity

// 심의 정보
@Entity
@Table(name="audits")
class Audit (



) : BaseEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
}