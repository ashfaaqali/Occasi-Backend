package com.occasi.application.controller

import com.occasi.application.dto.CardReviewResponse
import com.occasi.application.dto.CreateReviewRequest
import com.occasi.application.service.CardReviewService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/invitation-cards/{cardId}/reviews")
class CardReviewController(private val service: CardReviewService) {

    @PostMapping
    fun createReview(
        @PathVariable cardId: Long,
        @Valid @RequestBody request: CreateReviewRequest
    ): ResponseEntity<CardReviewResponse> {
        val response = service.createReview(cardId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @GetMapping
    fun getReviews(@PathVariable cardId: Long): ResponseEntity<List<CardReviewResponse>> {
        val response = service.getReviewsForCard(cardId)
        return ResponseEntity.ok(response)
    }
}
