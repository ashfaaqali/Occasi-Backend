package com.occasi.application.controller

import com.occasi.application.constants.BackendRoutes
import com.occasi.application.dto.PricingResponse
import com.occasi.application.dto.UpdatePricingRequest
import com.occasi.application.service.ArtistPricingService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping(BackendRoutes.ArtistPricing.BASE)
class ArtistPricingController(
    private val artistPricingService: ArtistPricingService
) {

    @PutMapping(BackendRoutes.ArtistPricing.PRICING)
    fun updatePricing(
        @Valid @RequestBody request: UpdatePricingRequest,
        authentication: Authentication
    ): ResponseEntity<PricingResponse> {
        val artistId = authentication.principal as Long
        val startingPrice = artistPricingService.updatePricing(artistId, request.pricingTiers)
        return ResponseEntity.ok(
            PricingResponse(message = "Pricing updated", startingPrice = startingPrice)
        )
    }
}
