package com.occasi.application.repository

import com.occasi.application.model.ArtistPortfolioImage
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ArtistPortfolioImageRepository : JpaRepository<ArtistPortfolioImage, Long> {
    fun findByArtistId(artistId: Long): List<ArtistPortfolioImage>
    fun countByArtistId(artistId: Long): Long
}
