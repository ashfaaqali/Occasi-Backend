package com.occasi.application.repository

import com.occasi.application.model.ArtistPricing
import com.occasi.application.model.ComplexityTier
import com.occasi.application.model.DesignType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ArtistPricingRepository : JpaRepository<ArtistPricing, Long> {
    fun findByArtistId(artistId: Long): List<ArtistPricing>
    fun findByArtistIdAndComplexityAndDesignType(artistId: Long, complexity: ComplexityTier, designType: DesignType): ArtistPricing?
    fun findByComplexityAndDesignType(complexity: ComplexityTier, designType: DesignType): List<ArtistPricing>
    fun deleteByArtistId(artistId: Long)
}
