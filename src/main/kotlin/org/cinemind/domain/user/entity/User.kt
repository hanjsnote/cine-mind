package org.cinemind.domain.user.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.cinemind.common.entity.BaseEntity
import org.cinemind.domain.chatlog.entity.ChatLog
import org.cinemind.domain.user.enums.UserRole

@Entity
@Table(name = "users")
class User(

    @Column(unique = true, nullable = false)
    val email: String,
    @Column(nullable = false)
    val password: String,
    @Enumerated(EnumType.STRING)
    val userRole: UserRole = UserRole.ROLE_USER

) : BaseEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    @OneToMany(mappedBy = "userId", fetch = FetchType.LAZY)
    val chatLogs: MutableList<ChatLog> = mutableListOf()
}