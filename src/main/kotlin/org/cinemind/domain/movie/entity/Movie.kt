package org.cinemind.domain.movie.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.cinemind.common.entity.BaseEntity
import java.time.LocalDate

// 영화 기본 정보
@Entity
@Table(name="movies")
class Movie (

    @Column(unique = true)
    val movieCd: String,        // 영화코드
    val movieNm: String,        // 영화명(국문)
    val movieNmEn: String,      // 영화명(영문)
    val showTm: Int,            // 상영시간
    val openDt: LocalDate,  // 개봉일
    val typeNm: String,         // 영화유형
    val watchGradeNm: String    // 관람등급

) : BaseEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    @OneToMany(mappedBy = "movie")
    // MutableSet 순서없는 중복 방지 컬렉션
    val moviePeople: MutableSet<MoviePeople> = mutableSetOf()

    @OneToMany(mappedBy = "movie")
    val movieGenre: MutableSet<MovieGenre> = mutableSetOf()

    @OneToMany(mappedBy = "movie")
    val movieCompany: MutableSet<MovieCompany> = mutableSetOf()

    @OneToMany(mappedBy = "movie")
    val boxOffice: MutableSet<BoxOffice> = mutableSetOf()

}