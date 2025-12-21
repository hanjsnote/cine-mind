package org.cinemind.domain.movie.entity

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.cinemind.common.entity.BaseEntity

// Movie, Company 매핑 테이블
@Entity
@Table(
    name="movie_companys",
    uniqueConstraints = [UniqueConstraint(
        name = "uk_movie_company_part",
        columnNames = ["movie_id", "company_id", "company_part_nm"]
    )]
)
class MovieCompany (

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id")
    val movie: Movie,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    val company: Company,

    val companyPartNm: String // 참여 영화사 분야명 ('제작', '배급'등)

) : BaseEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
}