package com.occasi.application.repository

import com.occasi.application.model.HennaDesign
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface HennaDesignRepository : JpaRepository<HennaDesign, Long> {
    fun findByPriceBetween(minPrice: Int, maxPrice: Int): List<HennaDesign>
    fun findByComplexity(complexity: String): List<HennaDesign>
    fun findByArtistId(artistId: Long): List<HennaDesign>
}
