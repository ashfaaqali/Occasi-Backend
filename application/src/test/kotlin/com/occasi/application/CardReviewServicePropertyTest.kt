package com.occasi.application

import com.occasi.application.dto.CreateReviewRequest
import com.occasi.application.exception.DuplicateReviewException
import com.occasi.application.exception.InvalidRatingException
import com.occasi.application.exception.ReviewNotEligibleException
import com.occasi.application.model.*
import com.occasi.application.repository.CardOrderRepository
import com.occasi.application.repository.CardReviewRepository
import com.occasi.application.repository.UserRepository
import com.occasi.application.service.CardReviewService
import com.occasi.application.service.InvitationCardService
import com.occasi.application.repository.InvitationCardRepository
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.PropertyTesting
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import org.mockito.kotlin.*
import java.time.LocalDateTime
import java.util.Optional

// Feature: invitation-cards-ordering
class CardReviewServicePropertyTest : StringSpec({

    beforeSpec {
        PropertyTesting.defaultIterationCount = 100
    }

    // Property 4: Review rating validation
    // **Validates: Requirements 3.2, 3.3, 3.6**
    "Property 4: review validation accepts iff overallRating in [1,5] and every non-null category in [1,5]" {
        val arbRating = Arb.int(-10..10)
        val arbNullableRating = Arb.int(-10..10).orNull(0.3)

        checkAll(
            arbRating,
            arbNullableRating,
            arbNullableRating,
            arbNullableRating,
            arbNullableRating
        ) { overall, design, print, paper, packaging ->
            val cardReviewRepository: CardReviewRepository = mock()
            val cardOrderRepository: CardOrderRepository = mock()
            val invitationCardService: InvitationCardService = mock()
            val userRepository: UserRepository = mock()

            val service = CardReviewService(
                cardReviewRepository, cardOrderRepository, invitationCardService, userRepository
            )

            val cardId = 1L
            val orderId = 100L
            val customerId = 10L

            // Set up a valid DELIVERED order so eligibility doesn't block us
            val order = CardOrder(
                id = orderId, cardId = cardId, customerId = customerId,
                quantity = 1, isSample = false, totalPrice = 100,
                status = OrderStatus.DELIVERED, deliveryAddress = "123 Test St"
            )
            whenever(cardOrderRepository.findById(orderId)).thenReturn(Optional.of(order))
            whenever(cardReviewRepository.findByOrderId(orderId)).thenReturn(null)
            whenever(userRepository.findById(customerId)).thenReturn(
                Optional.of(User(id = customerId, name = "Test User"))
            )
            doAnswer { (it.arguments[0] as CardReview).copy(id = 1L) }.whenever(cardReviewRepository).save(any())
            doNothing().whenever(invitationCardService).recalculateRating(any(), any())

            val request = CreateReviewRequest(
                orderId = orderId,
                customerId = customerId,
                overallRating = overall,
                designAccuracyRating = design,
                printQualityRating = print,
                paperFeelRating = paper,
                packagingRating = packaging
            )

            fun inRange(v: Int) = v in 1..5
            val allValid = inRange(overall) &&
                (design == null || inRange(design)) &&
                (print == null || inRange(print)) &&
                (paper == null || inRange(paper)) &&
                (packaging == null || inRange(packaging))

            if (allValid) {
                shouldNotThrowAny { service.createReview(cardId, request) }
            } else {
                shouldThrow<InvalidRatingException> { service.createReview(cardId, request) }
            }
        }
    }

    // Property 5: Review eligibility requires delivered order
    // **Validates: Requirements 4.1, 4.4**
    "Property 5: review creation succeeds only when customer has DELIVERED order" {
        val arbStatus = Arb.enum<OrderStatus>()

        checkAll(arbStatus) { status ->
            val cardReviewRepository: CardReviewRepository = mock()
            val cardOrderRepository: CardOrderRepository = mock()
            val invitationCardService: InvitationCardService = mock()
            val userRepository: UserRepository = mock()

            val service = CardReviewService(
                cardReviewRepository, cardOrderRepository, invitationCardService, userRepository
            )

            val cardId = 1L
            val orderId = 100L
            val customerId = 10L

            val order = CardOrder(
                id = orderId, cardId = cardId, customerId = customerId,
                quantity = 1, isSample = false, totalPrice = 100,
                status = status, deliveryAddress = "123 Test St"
            )
            whenever(cardOrderRepository.findById(orderId)).thenReturn(Optional.of(order))
            whenever(cardReviewRepository.findByOrderId(orderId)).thenReturn(null)
            whenever(userRepository.findById(customerId)).thenReturn(
                Optional.of(User(id = customerId, name = "Test User"))
            )
            doAnswer { (it.arguments[0] as CardReview).copy(id = 1L) }.whenever(cardReviewRepository).save(any())
            doNothing().whenever(invitationCardService).recalculateRating(any(), any())

            val request = CreateReviewRequest(
                orderId = orderId,
                customerId = customerId,
                overallRating = 4
            )

            if (status == OrderStatus.DELIVERED) {
                shouldNotThrowAny { service.createReview(cardId, request) }
            } else {
                shouldThrow<ReviewNotEligibleException> { service.createReview(cardId, request) }
            }
        }
    }

    // Property 6: One review per order
    // **Validates: Requirements 4.2**
    "Property 6: a second review for the same order is rejected" {
        val arbRating = Arb.int(1..5)

        checkAll(arbRating) { rating ->
            val cardReviewRepository: CardReviewRepository = mock()
            val cardOrderRepository: CardOrderRepository = mock()
            val invitationCardService: InvitationCardService = mock()
            val userRepository: UserRepository = mock()

            val service = CardReviewService(
                cardReviewRepository, cardOrderRepository, invitationCardService, userRepository
            )

            val cardId = 1L
            val orderId = 100L
            val customerId = 10L

            val order = CardOrder(
                id = orderId, cardId = cardId, customerId = customerId,
                quantity = 1, isSample = false, totalPrice = 100,
                status = OrderStatus.DELIVERED, deliveryAddress = "123 Test St"
            )
            whenever(cardOrderRepository.findById(orderId)).thenReturn(Optional.of(order))

            // An existing review already exists for this order
            val existingReview = CardReview(
                id = 1L, cardId = cardId, orderId = orderId, customerId = customerId,
                overallRating = 3
            )
            whenever(cardReviewRepository.findByOrderId(orderId)).thenReturn(existingReview)

            val request = CreateReviewRequest(
                orderId = orderId,
                customerId = customerId,
                overallRating = rating
            )

            shouldThrow<DuplicateReviewException> { service.createReview(cardId, request) }
        }
    }

    // Property 7: Rating recalculation correctness
    // **Validates: Requirements 4.3, 1.8**
    "Property 7: averageRating equals arithmetic mean and reviewCount equals count after sequence of ratings" {
        val arbRatings = Arb.list(Arb.int(1..5), 1..20)

        checkAll(arbRatings) { ratings ->
            val repository: InvitationCardRepository = mock()
            val card = InvitationCard(
                id = 1L, name = "Test Card", imageUrl = "https://example.com/img.png",
                price = 100, finish = "MATTE", printType = "DIGITAL",
                size = "5×7 inches", material = "CARDSTOCK", paperWeight = 300,
                averageRating = 0.0, reviewCount = 0
            )

            whenever(repository.findById(1L)).thenReturn(Optional.of(card))
            doAnswer { it.arguments[0] }.whenever(repository).save(any())

            val service = InvitationCardService(repository)

            for (rating in ratings) {
                service.recalculateRating(1L, rating)
            }

            val expectedMean = ratings.average()
            card.averageRating shouldBe (expectedMean plusOrMinus 0.0001)
            card.reviewCount shouldBe ratings.size
        }
    }

    // Property 10: Reviews sorted by createdAt descending
    // **Validates: Requirements 5.3**
    "Property 10: getReviewsForCard returns reviews sorted by createdAt descending" {
        val arbCount = Arb.int(0..10)

        checkAll(arbCount) { count ->
            val cardReviewRepository: CardReviewRepository = mock()
            val cardOrderRepository: CardOrderRepository = mock()
            val invitationCardService: InvitationCardService = mock()
            val userRepository: UserRepository = mock()

            val service = CardReviewService(
                cardReviewRepository, cardOrderRepository, invitationCardService, userRepository
            )

            val cardId = 1L
            val baseTime = LocalDateTime.of(2024, 1, 1, 0, 0)

            // Create reviews with various timestamps
            val reviews = (0 until count).map { i ->
                CardReview(
                    id = (i + 1).toLong(),
                    cardId = cardId,
                    orderId = (100 + i).toLong(),
                    customerId = (10 + i).toLong(),
                    overallRating = (i % 5) + 1,
                    createdAt = baseTime.plusHours(i.toLong())
                )
            }

            // Repository returns them sorted descending by createdAt (as per JPA method name)
            val sortedDesc = reviews.sortedByDescending { it.createdAt }
            whenever(cardReviewRepository.findByCardIdOrderByCreatedAtDesc(cardId)).thenReturn(sortedDesc)

            // Mock userRepository for each customer
            for (review in reviews) {
                whenever(userRepository.findById(review.customerId)).thenReturn(
                    Optional.of(User(id = review.customerId, name = "User ${review.customerId}"))
                )
            }

            val result = service.getReviewsForCard(cardId)

            result.size shouldBe count
            // Verify descending order by createdAt
            for (i in 0 until result.size - 1) {
                (result[i].createdAt >= result[i + 1].createdAt) shouldBe true
            }
        }
    }
})
