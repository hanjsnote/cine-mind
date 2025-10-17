package org.cinemind.domain.user.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.cinemind.common.entity.BaseEntity
import org.cinemind.domain.user.enums.UserRole

@Entity
@Table(name = "users")
class User(

    @Column(unique = true)
    val email: String,
    val password: String,
    @Enumerated(EnumType.STRING)
    val userRole: UserRole = UserRole.ROLE_USER

) : BaseEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
}
