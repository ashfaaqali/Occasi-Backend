package com.occasi.application.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class FavouriteRequest(
    @field:NotNull(message = "Item ID is required")
    val itemId: Long,
    @field:NotBlank(message = "Item type is required")
    val itemType: String
)
