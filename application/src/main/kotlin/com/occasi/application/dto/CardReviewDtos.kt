package com.occasi.application.dto

data class CreateReviewRequest(
    val orderId: Long,
    val customerId: Long,
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
