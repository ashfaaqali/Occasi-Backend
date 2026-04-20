package com.occasi.application.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
data class CardReview(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    var cardId: Long,
    var orderId: Long,
    var customerId: Long,
    var overallRating: Int,
    var designAccuracyRating: Int? = null,
    var printQualityRating: Int? = null,
    var paperFeelRating: Int? = null,
    var packagingRating: Int? = null,
    var reviewText: String? = null,
    var photoUrls: String? = null,
    var createdAt: LocalDateTime = LocalDateTime.now()
)
