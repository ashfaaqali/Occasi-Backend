package com.occasi.application.dto

import jakarta.validation.constraints.NotEmpty

data class AssociatePortfolioRequest(
    @field:NotEmpty(message = "imageUrls must not be empty")
    val imageUrls: List<String>
)

data class PortfolioResponse(
    val message: String,
    val totalImages: Int
)
