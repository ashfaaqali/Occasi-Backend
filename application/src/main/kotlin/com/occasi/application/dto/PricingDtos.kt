package com.occasi.application.dto

import jakarta.validation.constraints.NotEmpty

data class UpdatePricingRequest(
    @field:NotEmpty(message = "pricingTiers must not be empty")
    val pricingTiers: Map<String, Int>
)

data class PricingResponse(
    val message: String,
    val startingPrice: Int
)
