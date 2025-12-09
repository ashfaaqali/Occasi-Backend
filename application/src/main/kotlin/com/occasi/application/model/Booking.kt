package com.occasi.application.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
data class Booking(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne
    @JoinColumn(name = "user_id")
    var user: User,

    @ManyToOne
    @JoinColumn(name = "artist_id")
    var artist: HennaArtist,

    @ManyToOne
    @JoinColumn(name = "design_id")
    var design: HennaDesign,

    var status: String = "PENDING", // PENDING, CONFIRMED, COMPLETED, CANCELLED
    var bookingDate: LocalDateTime = LocalDateTime.now(),
    var price: Int
)
