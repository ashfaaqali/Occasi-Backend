package com.occasi.application.service

import com.occasi.application.constants.BackendMessages
import com.occasi.application.model.ArtistPortfolioImage
import com.occasi.application.repository.ArtistPortfolioImageRepository
import com.occasi.application.repository.HennaArtistRepository
import org.springframework.cache.annotation.CacheEvict
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ArtistPortfolioService(
    private val portfolioImageRepository: ArtistPortfolioImageRepository,
    private val hennaArtistRepository: HennaArtistRepository
) {

    companion object {
        const val MAX_PORTFOLIO_IMAGES = 6
    }

    @Transactional
    @CacheEvict(value = ["hennaArtists", "artistDetail"], allEntries = true)
    fun associateImages(artistId: Long, imageUrls: List<String>): Int {
        val currentCount = portfolioImageRepository.countByArtistId(artistId)

        if (currentCount + imageUrls.size > MAX_PORTFOLIO_IMAGES) {
            throw PortfolioLimitExceededException()
        }

        val artist = hennaArtistRepository.findById(artistId)
            .orElseThrow { IllegalArgumentException(BackendMessages.Artist.NOT_FOUND) }

        val images = imageUrls.map { url ->
            ArtistPortfolioImage(imageUrl = url).also { it.artist = artist }
        }

        portfolioImageRepository.saveAll(images)

        return (currentCount + imageUrls.size).toInt()
    }
}

class PortfolioLimitExceededException(
    message: String = BackendMessages.Upload.PORTFOLIO_LIMIT
) : RuntimeException(message)
