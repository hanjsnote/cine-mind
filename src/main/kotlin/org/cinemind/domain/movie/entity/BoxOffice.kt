package org.cinemind.domain.movie.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.cinemind.common.entity.BaseEntity

@Entity
@Table(name = "box_offices")
class BoxOffice (

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = false)
    val movie: Movie,

    // BoxOffice 기준 날짜
    @Column(nullable = false)
    val targetDt: String,

    val rank: String,          // 순위
    val saleAccess: String,   // 누적 매출액
    val audiAcc: String       // 누적 관객수

) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
}