package com.occasi.application.repository

import com.occasi.application.model.ArtistPricing
import com.occasi.application.model.ComplexityTier
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ArtistPricingRepository : JpaRepository<ArtistPricing, Long> {
    fun findByArtistId(artistId: Long): List<ArtistPricing>
    fun findByArtistIdAndComplexity(artistId: Long, complexity: ComplexityTier): ArtistPricing?
    fun findByComplexity(complexity: ComplexityTier): List<ArtistPricing>
}
