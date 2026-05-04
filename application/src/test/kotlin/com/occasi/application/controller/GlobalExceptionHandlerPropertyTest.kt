package com.occasi.application.controller

import com.occasi.application.exception.*
import com.occasi.application.service.*
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldMatch
import io.kotest.matchers.string.shouldNotContain
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import io.kotest.common.ExperimentalKotest
import jakarta.servlet.http.HttpServletRequest
import org.mockito.Mockito
import org.springframework.http.HttpStatus

// Feature: error-handling-crash-reporting, Properties 1, 2, 3
@OptIn(ExperimentalKotest::class)
class GlobalExceptionHandlerPropertyTest : StringSpec({

    val handler = GlobalExceptionHandler()

    val mockRequest: HttpServletRequest = Mockito.mock(HttpServletRequest::class.java).also {
        Mockito.`when`(it.method).thenReturn("GET")
        Mockito.`when`(it.requestURI).thenReturn("/test")
    }

    /** Arb for random non-empty message strings */
    val messageArb: Arb<String> = Arb.string(1..100, Codepoint.alphanumeric())

    /**
     * Maps each domain exception class to a factory that creates an instance from a message string.
     * These are the exceptions that have dedicated @ExceptionHandler methods in GlobalExceptionHandler.
     */
    val domainExceptionFactories: List<(String) -> RuntimeException> = listOf(
        { msg: String -> InvalidPhoneException(msg) },
        { msg: String -> InvalidOtpException(msg) },
        { msg: String -> OtpExpiredException(msg) },
        { msg: String -> OtpSendFailedException(msg) },
        { msg: String -> InvalidGoogleTokenException(msg) },
        { msg: String -> InvalidRefreshTokenException(msg) },
        { msg: String -> ArtistNotFoundException(msg) },
        { msg: String -> DuplicateArtistEmailException(msg) },
        { msg: String -> InvalidArtistCredentialsException(msg) },
        { msg: String -> InvalidArtistRefreshTokenException(msg) },
        { msg: String -> BookingNotFoundException(msg) },
        { msg: String -> InvalidStatusTransitionException(msg) },
        { msg: String -> PaymentVerificationException(msg) },
        { msg: String -> RefundFailedException(msg) },
        { msg: String -> InvalidBookingRequestException(msg) },
        { msg: String -> CardOrderNotFoundException(msg) },
        { msg: String -> DuplicateSampleOrderException(msg) },
        { msg: String -> InvalidOrderQuantityException(msg) },
        { msg: String -> InvalidOrderStatusTransitionException(msg) },
        { msg: String -> ReviewNotEligibleException(msg) },
        { msg: String -> DuplicateReviewException(msg) },
        { msg: String -> InvalidRatingException(msg) },
    )

    /** Arb that generates a random domain exception with a random message */
    val domainExceptionArb: Arb<RuntimeException> = arbitrary { rs ->
        val msg = messageArb.bind()
        val factoryIndex = rs.random.nextInt(domainExceptionFactories.size)
        domainExceptionFactories[factoryIndex](msg)
    }

    /**
     * Dispatches a domain exception to the correct handler method on GlobalExceptionHandler.
     * Returns the ResponseEntity from the handler.
     */
    fun dispatchToHandler(
        ex: RuntimeException,
        request: HttpServletRequest
    ) = when (ex) {
        is InvalidPhoneException -> handler.handleInvalidPhone(ex, request)
        is InvalidOtpException -> handler.handleInvalidOtp(ex, request)
        is OtpExpiredException -> handler.handleOtpExpired(ex, request)
        is OtpSendFailedException -> handler.handleOtpSendFailed(ex, request)
        is InvalidGoogleTokenException -> handler.handleInvalidGoogleToken(ex, request)
        is InvalidRefreshTokenException -> handler.handleInvalidRefreshToken(ex, request)
        is ArtistNotFoundException -> handler.handleArtistNotFound(ex, request)
        is DuplicateArtistEmailException -> handler.handleDuplicateArtistEmail(ex, request)
        is InvalidArtistCredentialsException -> handler.handleInvalidArtistCredentials(ex, request)
        is InvalidArtistRefreshTokenException -> handler.handleInvalidArtistRefreshToken(ex, request)
        is BookingNotFoundException -> handler.handleBookingNotFound(ex, request)
        is InvalidStatusTransitionException -> handler.handleInvalidTransition(ex, request)
        is PaymentVerificationException -> handler.handlePaymentVerification(ex, request)
        is RefundFailedException -> handler.handleRefundFailed(ex, request)
        is InvalidBookingRequestException -> handler.handleInvalidBookingRequest(ex, request)
        is CardOrderNotFoundException -> handler.handleCardOrderNotFound(ex, request)
        is DuplicateSampleOrderException -> handler.handleDuplicateSampleOrder(ex, request)
        is InvalidOrderQuantityException -> handler.handleInvalidOrderQuantity(ex, request)
        is InvalidOrderStatusTransitionException -> handler.handleInvalidOrderStatusTransition(ex, request)
        is ReviewNotEligibleException -> handler.handleReviewNotEligible(ex, request)
        is DuplicateReviewException -> handler.handleDuplicateReview(ex, request)
        is InvalidRatingException -> handler.handleInvalidRating(ex, request)
        else -> handler.handleUnexpected(ex, request)
    }

    /**
     * Maps an ErrorCode enum entry to the corresponding exception factory.
     * Only includes domain exceptions that have dedicated handlers (excludes
     * VALIDATION_ERROR, MALFORMED_REQUEST, FILE_TOO_LARGE, INTERNAL_ERROR which
     * are handled by Spring-specific or catch-all handlers).
     */
    val errorCodeToFactory: Map<ErrorCode, (String) -> RuntimeException> = mapOf(
        ErrorCode.INVALID_PHONE to { msg: String -> InvalidPhoneException(msg) },
        ErrorCode.INVALID_OTP to { msg: String -> InvalidOtpException(msg) },
        ErrorCode.OTP_EXPIRED to { msg: String -> OtpExpiredException(msg) },
        ErrorCode.OTP_SEND_FAILED to { msg: String -> OtpSendFailedException(msg) },
        ErrorCode.INVALID_GOOGLE_TOKEN to { msg: String -> InvalidGoogleTokenException(msg) },
        ErrorCode.INVALID_REFRESH_TOKEN to { msg: String -> InvalidRefreshTokenException(msg) },
        ErrorCode.ARTIST_NOT_FOUND to { msg: String -> ArtistNotFoundException(msg) },
        ErrorCode.DUPLICATE_ARTIST_EMAIL to { msg: String -> DuplicateArtistEmailException(msg) },
        ErrorCode.INVALID_ARTIST_CREDENTIALS to { msg: String -> InvalidArtistCredentialsException(msg) },
        ErrorCode.INVALID_ARTIST_REFRESH_TOKEN to { msg: String -> InvalidArtistRefreshTokenException(msg) },
        ErrorCode.BOOKING_NOT_FOUND to { msg: String -> BookingNotFoundException(msg) },
        ErrorCode.INVALID_STATUS_TRANSITION to { msg: String -> InvalidStatusTransitionException(msg) },
        ErrorCode.PAYMENT_VERIFICATION_FAILED to { msg: String -> PaymentVerificationException(msg) },
        ErrorCode.REFUND_FAILED to { msg: String -> RefundFailedException(msg) },
        ErrorCode.INVALID_BOOKING_REQUEST to { msg: String -> InvalidBookingRequestException(msg) },
        ErrorCode.CARD_ORDER_NOT_FOUND to { msg: String -> CardOrderNotFoundException(msg) },
        ErrorCode.DUPLICATE_SAMPLE_ORDER to { msg: String -> DuplicateSampleOrderException(msg) },
        ErrorCode.INVALID_ORDER_QUANTITY to { msg: String -> InvalidOrderQuantityException(msg) },
        ErrorCode.INVALID_ORDER_STATUS_TRANSITION to { msg: String -> InvalidOrderStatusTransitionException(msg) },
        ErrorCode.REVIEW_NOT_ELIGIBLE to { msg: String -> ReviewNotEligibleException(msg) },
        ErrorCode.DUPLICATE_REVIEW to { msg: String -> DuplicateReviewException(msg) },
        ErrorCode.INVALID_RATING to { msg: String -> InvalidRatingException(msg) },
    )

    val upperSnakeCasePattern = Regex("^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$")

    // Feature: error-handling-crash-reporting, Property 1: Error response structure and code format
    // **Validates: Requirements 1.1, 1.4**
    "Property 1: Error response structure and code format - all domain exceptions produce non-empty error and UPPER_SNAKE_CASE code" {
        checkAll(PropTestConfig(minSuccess = 100), domainExceptionArb) { ex ->
            val response = dispatchToHandler(ex, mockRequest)
            val body = response.body!!

            body.error shouldNotBe ""
            body.error.isNotEmpty() shouldBe true
            body.code shouldMatch upperSnakeCasePattern
        }
    }

    // Feature: error-handling-crash-reporting, Property 2: Exception-to-status-code mapping
    // **Validates: Requirements 1.2, 11.1**
    "Property 2: Exception-to-status-code mapping - each ErrorCode enum entry maps to correct HTTP status and response code" {
        checkAll(PropTestConfig(minSuccess = 100), messageArb) { msg ->
            for ((errorCode, factory) in errorCodeToFactory) {
                val ex = factory(msg)
                val response = dispatchToHandler(ex, mockRequest)
                val body = response.body!!

                response.statusCode shouldBe errorCode.httpStatus
                body.code shouldBe errorCode.name
            }
        }
    }

    // Feature: error-handling-crash-reporting, Property 3: No stack trace leakage in catch-all handler
    // **Validates: Requirements 1.3**
    "Property 3: No stack trace leakage in catch-all handler - RuntimeExceptions produce 500/INTERNAL_ERROR without stack trace patterns" {
        // Generate messages that could look like stack traces to ensure they don't leak
        val stackTraceMessageArb: Arb<String> = Arb.choice(
            messageArb,
            Arb.of(
                "at com.occasi.application.service.BookingService.create(BookingService.kt:42)",
                "java.lang.NullPointerException at SomeClass.method(SomeClass.java:10)",
                "caused by: RuntimeException at org.example.Foo.bar(Foo.kt:99)",
                "com.occasi.application.SomeClass.someMethod",
                "Exception in thread \"main\" java.lang.RuntimeException",
                "\tat org.springframework.web.servlet.FrameworkServlet.service(FrameworkServlet.java:97)"
            )
        )

        checkAll(PropTestConfig(minSuccess = 100), stackTraceMessageArb) { msg ->
            val ex = RuntimeException(msg)
            val response = handler.handleUnexpected(ex, mockRequest)
            val body = response.body!!

            response.statusCode shouldBe HttpStatus.INTERNAL_SERVER_ERROR
            body.code shouldBe "INTERNAL_ERROR"

            // The error field must not contain stack trace patterns
            body.error shouldNotContain "\tat "
            body.error shouldNotContain ".kt:"
            body.error shouldNotContain ".java:"
            body.error shouldNotContain "Exception in thread"
            // The catch-all returns a generic message, not the exception message
            body.error shouldBe "An unexpected error occurred"
        }
    }
})
