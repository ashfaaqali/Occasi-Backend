package com.occasi.application.service

import com.occasi.application.model.HennaDesign
import com.occasi.application.model.ComplexityTier
import com.occasi.application.model.DesignType
import com.occasi.application.repository.HennaDesignRepository
import com.occasi.application.repository.ArtistPricingRepository
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class HennaDesignService(
    private val repository: HennaDesignRepository,
    private val artistPricingRepository: ArtistPricingRepository
) {

    @Cacheable("hennaDesigns")
    fun getAllDesigns(): List<HennaDesign> {
        val designs = repository.findAll()
        populateStartingPrices(designs)
        return designs
    }

    fun getDesignsByComplexity(complexity: String): List<HennaDesign> {
        val designs = repository.findByComplexity(complexity)
        populateStartingPrices(designs)
        return designs
    }

    @Cacheable("designDetail", key = "#id")
    fun getDesignById(id: Long): HennaDesign? {
        val design = repository.findById(id).orElse(null) ?: return null
        val tier = try {
            ComplexityTier.valueOf(design.complexity.uppercase())
        } catch (_: Exception) {
            null
        }
        if (tier != null) {
            val resolvedPrice = artistPricingRepository.findByComplexityAndDesignType(tier, design.designType)
                .map { it.price }
                .minOrNull()
            design.startingPrice = if (resolvedPrice != null && resolvedPrice > 0) {
                resolvedPrice
            } else {
                when (tier) {
                    ComplexityTier.SIMPLE -> if (design.designType == DesignType.HAND) 100 else 150
                    ComplexityTier.MID -> if (design.designType == DesignType.HAND) 200 else 250
                    ComplexityTier.COMPLEX -> if (design.designType == DesignType.HAND) 300 else 400
                    ComplexityTier.BRIDAL -> if (design.designType == DesignType.HAND) 800 else 1200
                }
            }
        }
        return design
    }

    private fun populateStartingPrices(designs: List<HennaDesign>) {
        val pricings = artistPricingRepository.findAll()
        val minPrices = pricings.groupBy { it.designType to it.complexity }
            .mapValues { (_, list) -> list.minOfOrNull { it.price } ?: 0 }

        designs.forEach { design ->
            val tier = try {
                ComplexityTier.valueOf(design.complexity.uppercase())
            } catch (_: Exception) {
                null
            }
            if (tier != null) {
                val resolvedPrice = minPrices[design.designType to tier]
                design.startingPrice = if (resolvedPrice != null && resolvedPrice > 0) {
                    resolvedPrice
                } else {
                    when (tier) {
                        ComplexityTier.SIMPLE -> if (design.designType == DesignType.HAND) 100 else 150
                        ComplexityTier.MID -> if (design.designType == DesignType.HAND) 200 else 250
                        ComplexityTier.COMPLEX -> if (design.designType == DesignType.HAND) 300 else 400
                        ComplexityTier.BRIDAL -> if (design.designType == DesignType.HAND) 800 else 1200
                    }
                }
            }
        }
    }

    @CacheEvict(value = ["hennaDesigns", "designDetail"], allEntries = true)
    fun saveDesign(design: HennaDesign): HennaDesign {
        return repository.save(design)
    }

    @CacheEvict(value = ["hennaDesigns", "designDetail"], allEntries = true)
    fun deleteDesign(id: Long) = repository.deleteById(id)
}
