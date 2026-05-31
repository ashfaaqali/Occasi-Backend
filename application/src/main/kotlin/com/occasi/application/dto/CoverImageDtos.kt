package com.occasi.application.dto

import jakarta.validation.constraints.NotBlank

data class UpdateCoverImageRequest(
    @field:NotBlank(message = "coverImageUrl must not be blank")
    val coverImageUrl: String
)

data class CoverImageResponse(
    val message: String,
    val coverImageUrl: String
)
