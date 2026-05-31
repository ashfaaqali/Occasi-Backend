package com.occasi.application.model

import jakarta.persistence.*
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import java.time.LocalDateTime

@Entity
@Table(name = "refresh_token")
data class RefreshToken(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    var token: String,

    @ManyToOne
    @JoinColumn(name = "user_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    var user: User,

    var expiresAt: LocalDateTime,
    var createdAt: LocalDateTime = LocalDateTime.now()
)
