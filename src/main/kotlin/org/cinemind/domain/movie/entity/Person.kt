package org.cinemind.domain.movie.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.cinemind.common.entity.BaseEntity

// 배우, 감독 정보 / 역할에 상관없이 이름만 들어감 (중복 없음)
@Entity
@Table(name="persons")
class Person (

    val nameKr: String,     // 이름(국문)
    val nameEn: String      // 이름(영문)

) : BaseEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    @OneToMany(mappedBy = "person")
    // MutableSet 순서없는 중복 방지 컬렉션
    val moviePerson: MutableSet<MoviePerson> = mutableSetOf()
}