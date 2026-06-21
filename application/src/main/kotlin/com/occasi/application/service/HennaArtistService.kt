package com.occasi.application.service

import com.occasi.application.dto.ArtistRegistrationRequest
import com.occasi.application.dto.ArtistWithPriceDto
import com.occasi.application.model.ArtistPortfolioImage
import com.occasi.application.model.ComplexityTier
import com.occasi.application.model.DesignType
import com.occasi.application.model.HennaArtist
import com.occasi.application.repository.ArtistPricingRepository
import com.occasi.application.repository.HennaArtistRepository
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class HennaArtistService(
    private val repository: HennaArtistRepository,
    private val artistPricingRepository: ArtistPricingRepository
) {

    @Cacheable("hennaArtists")
    fun getAllHennaArtists(): List<HennaArtist> = repository.findAll()

    @Cacheable("hennaArtistsByCity", key = "#city")
    fun getArtistsByCity(city: String): List<HennaArtist> = repository.findByCityNameIgnoreCase(city)

    @Cacheable("availableCities")
    fun getAvailableCities(): List<String> = repository.findDistinctCityNames()

    @Cacheable("artistDetail", key = "#id")
    fun getArtistById(id: Long): HennaArtist? = repository.findById(id).orElse(null)

    @CacheEvict(value = ["hennaArtists", "hennaArtistsByCity", "availableCities", "artistDetail", "hennaDesigns", "designDetail"], allEntries = true)
    fun registerArtist(request: ArtistRegistrationRequest): HennaArtist {
        val artist = HennaArtist(
            name = request.name,
            email = request.email,
            mobileNumber = request.mobileNumber,
            cityName = request.cityName ?: "Unknown",
            location = request.location ?: "Unknown"
        )
        val portfolioImages = request.portfolioImageUrls.map { url ->
            ArtistPortfolioImage(imageUrl = url).also { it.artist = artist }
        }
        artist.portfolioImages = portfolioImages
        return repository.save(artist)
    }

    fun getArtistsForComplexity(complexity: String, designTypeStr: String = "HAND"): List<ArtistWithPriceDto> {
        val tier = ComplexityTier.valueOf(complexity.uppercase())
        val designType = DesignType.valueOf(designTypeStr.uppercase())
        return artistPricingRepository.findByComplexityAndDesignType(tier, designType)
            .map { pricing ->
                ArtistWithPriceDto(
                    artistId = pricing.artist.id!!,
                    artistName = pricing.artist.name,
                    coverImage = pricing.artist.coverImage,
                    rating = pricing.artist.rating,
                    priceForComplexity = pricing.price
                )
            }
            .sortedBy { it.priceForComplexity }
    }
}