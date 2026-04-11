package com.occasi.application.model

import com.fasterxml.jackson.annotation.JsonBackReference
import jakarta.persistence.*

@Entity
@Table(uniqueConstraints = [UniqueConstraint(columnNames = ["artist_id", "complexity"])])
data class ArtistPricing(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_id", nullable = false)
    @JsonBackReference("artist-pricing")
    var artist: HennaArtist,

    @Enumerated(EnumType.STRING)
    var complexity: ComplexityTier,

    var price: Int
) {
    init {
        require(price > 0) { "Price must be greater than zero" }
    }
}
