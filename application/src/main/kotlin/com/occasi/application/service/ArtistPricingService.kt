package com.occasi.application.service

import com.occasi.application.constants.BackendMessages
import com.occasi.application.model.ArtistPricing
import com.occasi.application.model.ComplexityTier
import com.occasi.application.repository.ArtistPricingRepository
import com.occasi.application.repository.HennaArtistRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ArtistPricingService(
    private val artistPricingRepository: ArtistPricingRepository,
    private val hennaArtistRepository: HennaArtistRepository
) {

    @Transactional
    fun updatePricing(artistId: Long, pricingTiers: Map<String, Int>): Int {
        // Validate all values are positive
        if (pricingTiers.values.any { it <= 0 }) {
            throw InvalidPricingException(BackendMessages.Validation.INVALID_PRICING)
        }

        // Validate all keys are valid ComplexityTier enum values
        pricingTiers.keys.forEach { key ->
            if (ComplexityTier.entries.none { it.name == key.uppercase() }) {
                throw InvalidPricingException("Invalid complexity tier: $key")
            }
        }

        val artist = hennaArtistRepository.findById(artistId)
            .orElseThrow { IllegalArgumentException(BackendMessages.Artist.NOT_FOUND) }

        // Upsert: delete existing rows, insert new ones (within transaction)
        artistPricingRepository.deleteByArtistId(artistId)

        val pricingEntities = pricingTiers.map { (complexity, price) ->
            ArtistPricing(
                artist = artist,
                complexity = ComplexityTier.valueOf(complexity.uppercase()),
                price = price
            )
        }
        artistPricingRepository.saveAll(pricingEntities)

        // Update startingPrice to minimum of provided prices
        val startingPrice = pricingTiers.values.min()
        artist.startingPrice = startingPrice
        hennaArtistRepository.save(artist)

        return startingPrice
    }
}

class InvalidPricingException(
    message: String = BackendMessages.Validation.INVALID_PRICING
) : RuntimeException(message)
