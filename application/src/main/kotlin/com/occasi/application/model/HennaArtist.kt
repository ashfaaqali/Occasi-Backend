package com.occasi.application.model

import com.fasterxml.jackson.annotation.JsonManagedReference
import jakarta.persistence.*
import java.time.Instant

@Entity
data class HennaArtist(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    
    var name: String,
    var email: String,
    var mobileNumber: String,
    var cityName: String,
    var location: String,
    var rating: Short = 0,
    var reviews: Int = 0,
    @Column(columnDefinition = "TEXT")
    var coverImage: String? = null,
    var startingPrice: Int = 0,
    var passwordHash: String? = null,
    var updatedAt: Instant = Instant.now()
) {
    @OneToMany(mappedBy = "artist", cascade = [CascadeType.ALL], orphanRemoval = true)
    @JsonManagedReference("artist-portfolio")
    var portfolioImages: List<ArtistPortfolioImage> = ArrayList()

    @OneToMany(mappedBy = "artist", cascade = [CascadeType.ALL], orphanRemoval = true)
    @JsonManagedReference("artist-pricing")
    var pricingTiers: List<ArtistPricing> = ArrayList()
}
