package com.occasi.application.service

import com.occasi.application.dto.CardReviewResponse
import com.occasi.application.dto.CreateReviewRequest
import com.occasi.application.exception.DuplicateReviewException
import com.occasi.application.exception.InvalidRatingException
import com.occasi.application.exception.ReviewNotEligibleException
import com.occasi.application.model.CardReview
import com.occasi.application.model.OrderStatus
import com.occasi.application.repository.CardOrderRepository
import com.occasi.application.repository.CardReviewRepository
import com.occasi.application.repository.UserRepository
import com.occasi.application.util.InputSanitizer
import org.springframework.stereotype.Service
import java.time.format.DateTimeFormatter

@Service
class CardReviewService(
    private val cardReviewRepository: CardReviewRepository,
    private val cardOrderRepository: CardOrderRepository,
    private val invitationCardService: InvitationCardService,
    private val userRepository: UserRepository
) {

    fun createReview(cardId: Long, request: CreateReviewRequest): CardReviewResponse {
        // Validate overall rating 1-5
        validateRating(request.overallRating)

        // Validate each non-null category rating 1-5
        request.designAccuracyRating?.let { validateRating(it) }
        request.printQualityRating?.let { validateRating(it) }
        request.paperFeelRating?.let { validateRating(it) }
        request.packagingRating?.let { validateRating(it) }

        // Verify customer has a DELIVERED order for this card
        val order = cardOrderRepository.findById(request.orderId)
            .orElseThrow { ReviewNotEligibleException("You must have a delivered order to review this card") }

        if (order.cardId != cardId || order.customerId != request.customerId || order.status != OrderStatus.DELIVERED) {
            throw ReviewNotEligibleException("You must have a delivered order to review this card")
        }

        // Verify no existing review for this order
        val existingReview = cardReviewRepository.findByOrderId(request.orderId)
        if (existingReview != null) {
            throw DuplicateReviewException("A review already exists for this order")
        }

        // Sanitize free-form text fields
        val sanitizedReviewText = request.reviewText?.let { InputSanitizer.sanitize(it) }

        // Save review
        val review = CardReview(
            cardId = cardId,
            orderId = request.orderId,
            customerId = request.customerId,
            overallRating = request.overallRating,
            designAccuracyRating = request.designAccuracyRating,
            printQualityRating = request.printQualityRating,
            paperFeelRating = request.paperFeelRating,
            packagingRating = request.packagingRating,
            reviewText = sanitizedReviewText,
            photoUrls = request.photoUrls?.joinToString(",")
        )

        val savedReview = cardReviewRepository.save(review)

        // Recalculate card averageRating and increment reviewCount
        invitationCardService.recalculateRating(cardId, request.overallRating)

        val customerName = getCustomerName(request.customerId)
        return toResponse(savedReview, customerName)
    }

    fun getReviewsForCard(cardId: Long): List<CardReviewResponse> {
        return cardReviewRepository.findByCardIdOrderByCreatedAtDesc(cardId).map { review ->
            val customerName = getCustomerName(review.customerId)
            toResponse(review, customerName)
        }
    }

    private fun validateRating(rating: Int) {
        if (rating < 1 || rating > 5) {
            throw InvalidRatingException("Rating must be between 1 and 5")
        }
    }

    private fun getCustomerName(customerId: Long): String {
        return userRepository.findById(customerId)
            .map { it.name.ifBlank { "Customer $customerId" } }
            .orElse("Customer $customerId")
    }

    private fun toResponse(review: CardReview, customerName: String): CardReviewResponse {
        return CardReviewResponse(
            id = review.id!!,
            cardId = review.cardId,
            orderId = review.orderId,
            customerName = customerName,
            overallRating = review.overallRating,
            designAccuracyRating = review.designAccuracyRating,
            printQualityRating = review.printQualityRating,
            paperFeelRating = review.paperFeelRating,
            packagingRating = review.packagingRating,
            reviewText = review.reviewText,
            photoUrls = review.photoUrls?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
            createdAt = review.createdAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        )
    }
}
