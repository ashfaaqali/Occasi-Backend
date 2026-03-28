package com.occasi.application.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "refresh_tokens")
data class RefreshToken(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    var token: String,

    @ManyToOne
    @JoinColumn(name = "user_id")
    var user: User,

    var expiresAt: LocalDateTime,
    var createdAt: LocalDateTime = LocalDateTime.now()
)
