package com.occasi.application.repository

import com.occasi.application.model.CardReview
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CardReviewRepository : JpaRepository<CardReview, Long> {
    fun findByCardIdOrderByCreatedAtDesc(cardId: Long): List<CardReview>
    fun findByOrderId(orderId: Long): CardReview?
}
