package com.occasi.application.model

import jakarta.persistence.*
import com.fasterxml.jackson.annotation.JsonBackReference

@Entity
data class HennaDesign(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    var imageUrl: String,
    var price: Int,
    var complexity: String, // "Simple", "Mid", "Complex"
    var category: String, // "Bridal", "Party", etc.
    var likes: Int = 0,
    var numberOfPeopleBooked: Int = 0
) {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_id")
    @JsonBackReference
    var artist: HennaArtist? = null
}
