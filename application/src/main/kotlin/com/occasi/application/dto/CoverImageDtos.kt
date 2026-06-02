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

data class SubmitKycRequest(
    @field:NotBlank(message = "idFrontUrl must not be blank")
    val idFrontUrl: String,
    @field:NotBlank(message = "idBackUrl must not be blank")
    val idBackUrl: String
)
