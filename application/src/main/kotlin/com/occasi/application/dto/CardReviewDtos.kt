package com.occasi.application.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

data class CreateReviewRequest(
    val orderId: Long,
    val customerId: Long,
    @field:Min(value = 1, message = "Rating must be at least 1")
    @field:Max(value = 5, message = "Rating must be at most 5")
    val overallRating: Int,
    val designAccuracyRating: Int? = null,
    val printQualityRating: Int? = null,
    val paperFeelRating: Int? = null,
    val packagingRating: Int? = null,
    val reviewText: String? = null,
    val photoUrls: List<String>? = null
)

data class CardReviewResponse(
    val id: Long,
    val cardId: Long,
    val orderId: Long,
    val customerName: String,
    val overallRating: Int,
    val designAccuracyRating: Int?,
    val printQualityRating: Int?,
    val paperFeelRating: Int?,
    val packagingRating: Int?,
    val reviewText: String?,
    val photoUrls: List<String>,
    val createdAt: String
)
