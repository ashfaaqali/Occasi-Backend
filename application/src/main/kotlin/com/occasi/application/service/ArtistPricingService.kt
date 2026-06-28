package com.occasi.application.service

import com.occasi.application.constants.BackendMessages
import com.occasi.application.model.ArtistPricing
import com.occasi.application.model.ComplexityTier
import com.occasi.application.model.DesignType
import com.occasi.application.repository.ArtistPricingRepository
import com.occasi.application.repository.HennaArtistRepository
import org.springframework.cache.annotation.CacheEvict
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
 
@Service
class ArtistPricingService(
    private val artistPricingRepository: ArtistPricingRepository,
    private val hennaArtistRepository: HennaArtistRepository
) {
 
    @Transactional
    @CacheEvict(value = ["hennaArtists", "artistDetail", "hennaDesigns", "designDetail"], allEntries = true)
    fun updatePricing(artistId: Long, pricingTiers: Map<String, Int>, bridalPrice: Int): Int {
        // Validate all values are positive
        if (pricingTiers.values.any { it <= 0 }) {
            throw InvalidPricingException(BackendMessages.Validation.INVALID_PRICING)
        }

        // Validate all keys are valid compound keys of format [DesignType]_[ComplexityTier]
        pricingTiers.keys.forEach { key ->
            val parts = key.split("_")
            if (parts.size != 2) {
                throw InvalidPricingException("Invalid pricing key format: $key. Expected DESIGNTYPE_COMPLEXITY")
            }
            val designTypeStr = parts[0].uppercase()
            val complexityStr = parts[1].uppercase()
            if (DesignType.entries.none { it.name == designTypeStr }) {
                throw InvalidPricingException("Invalid design type: $designTypeStr")
            }
            if (ComplexityTier.entries.none { it.name == complexityStr }) {
                throw InvalidPricingException("Invalid complexity tier: $complexityStr")
            }
        }

        val artist = hennaArtistRepository.findById(artistId)
            .orElseThrow { IllegalArgumentException(BackendMessages.Artist.NOT_FOUND) }

        artist.bridalPrice = bridalPrice

        // Upsert: delete existing rows, insert new ones (within transaction)
        artistPricingRepository.deleteByArtistId(artistId)
        artistPricingRepository.flush()

        val pricingEntities = pricingTiers.map { (key, price) ->
            val parts = key.split("_")
            ArtistPricing(
                artist = artist,
                complexity = ComplexityTier.valueOf(parts[1].uppercase()),
                designType = DesignType.valueOf(parts[0].uppercase()),
                price = price
            )
        }
        artistPricingRepository.saveAll(pricingEntities)

        // Update startingPrice to minimum of provided prices
        val startingPrice = pricingTiers.values.minOrNull() ?: 0
        artist.startingPrice = startingPrice
        artist.updatedAt = java.time.Instant.now()
        hennaArtistRepository.save(artist)

        return startingPrice
    }
}

class InvalidPricingException(
    message: String = BackendMessages.Validation.INVALID_PRICING
) : RuntimeException(message)
