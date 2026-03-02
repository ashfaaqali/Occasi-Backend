package com.occasi.application.model

import jakarta.persistence.*
import com.fasterxml.jackson.annotation.JsonBackReference

@Entity
data class HennaDesign(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    var imageUrl: String,
    var name: String = "",           // Design name for display
    var price: Int,
    var complexity: String,          // "Simple", "Mid", "Complex"
    var tags: String = "",           // Comma-separated tags e.g. "BRIDAL,ARABIC,WEDDING"
    var likes: Int = 0,
    var numberOfPeopleBooked: Int = 0
) {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_id")
    @JsonBackReference
    var artist: HennaArtist? = null

    init {
        require(name.isNotBlank()) { "Design name must not be empty" }
        require(complexity in listOf("Simple", "Mid", "Complex")) { "Complexity must be Simple, Mid, or Complex" }
        require(tags.isNotBlank()) { "Tags must not be empty" }
        tags.split(",").forEach { tag ->
            require(tag.trim().isNotEmpty()) { "Each tag must be non-empty after trimming" }
        }
    }
}
