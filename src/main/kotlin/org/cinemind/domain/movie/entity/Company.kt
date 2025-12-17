package org.cinemind.domain.movie.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.cinemind.common.entity.BaseEntity

// 제작사 정보
@Entity
@Table(name="companies")
class Company (

    val companyCd: String,      // 참여 영화사 코드
    @Column(unique = true)
    val companyNm: String,      // 참여 영화사명(국문)
    val companyNmEn: String?,   // 참여 영화사명(영문)

) : BaseEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    @OneToMany(mappedBy = "company")
    // MutableSet 순서없는 중복 방지 컬렉션
    val movieCompany: MutableSet<MovieCompany> = mutableSetOf()
}