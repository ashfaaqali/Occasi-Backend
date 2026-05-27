package com.occasi.application.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "artist_refresh_token")
data class ArtistRefreshToken(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    var token: String,

    @ManyToOne
    @JoinColumn(name = "artist_id")
    var artist: HennaArtist,

    var expiresAt: LocalDateTime,
    var createdAt: LocalDateTime = LocalDateTime.now()
)
