package com.occasi.application.controller

import com.occasi.application.constants.BackendMessages
import com.occasi.application.dto.ErrorResponse
import com.occasi.application.exception.*
import com.occasi.application.service.*
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.multipart.MaxUploadSizeExceededException

/**
 * Global exception handler that intercepts all unhandled exceptions thrown by controllers
 * and converts them into structured [ErrorResponse] JSON responses.
 *
 * Logging strategy:
 * - 4xx client errors are logged at WARN level (no stack trace).
 * - 5xx server errors are logged at ERROR level (with stack trace).
 * - The catch-all handler never leaks stack traces in the response body.
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    // Auth exceptions

    @ExceptionHandler(InvalidPhoneException::class)
    fun handleInvalidPhone(
        ex: InvalidPhoneException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("INVALID_PHONE: {} {}", request.method, request.requestURI)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(error = ex.message ?: BackendMessages.Auth.INVALID_PHONE, code = "INVALID_PHONE"))
    }

    @ExceptionHandler(InvalidOtpException::class)
    fun handleInvalidOtp(
        ex: InvalidOtpException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("INVALID_OTP: {} {}", request.method, request.requestURI)
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ErrorResponse(error = ex.message ?: BackendMessages.Auth.INVALID_OTP, code = "INVALID_OTP"))
    }

    @ExceptionHandler(OtpExpiredException::class)
    fun handleOtpExpired(
        ex: OtpExpiredException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("OTP_EXPIRED: {} {}", request.method, request.requestURI)
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ErrorResponse(error = ex.message ?: BackendMessages.Auth.OTP_EXPIRED, code = "OTP_EXPIRED"))
    }

    @ExceptionHandler(OtpSendFailedException::class)
    fun handleOtpSendFailed(
        ex: OtpSendFailedException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("OTP_SEND_FAILED: {} {} - {}", request.method, request.requestURI, ex.message, ex)
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(ErrorResponse(error = ex.message ?: BackendMessages.Auth.OTP_SEND_FAILED, code = "OTP_SEND_FAILED"))
    }

    @ExceptionHandler(InvalidGoogleTokenException::class)
    fun handleInvalidGoogleToken(
        ex: InvalidGoogleTokenException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("INVALID_GOOGLE_TOKEN: {} {}", request.method, request.requestURI)
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ErrorResponse(error = ex.message ?: BackendMessages.Auth.INVALID_GOOGLE_TOKEN, code = "INVALID_GOOGLE_TOKEN"))
    }

    @ExceptionHandler(InvalidRefreshTokenException::class)
    fun handleInvalidRefreshToken(
        ex: InvalidRefreshTokenException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("INVALID_REFRESH_TOKEN: {} {}", request.method, request.requestURI)
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ErrorResponse(error = ex.message ?: BackendMessages.Auth.SESSION_EXPIRED, code = "INVALID_REFRESH_TOKEN"))
    }

    // Artist exceptions

    @ExceptionHandler(ArtistNotFoundException::class)
    fun handleArtistNotFound(
        ex: ArtistNotFoundException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("ARTIST_NOT_FOUND: {} {}", request.method, request.requestURI)
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse(error = ex.message ?: BackendMessages.Artist.NOT_FOUND, code = "ARTIST_NOT_FOUND"))
    }

    @ExceptionHandler(DuplicateArtistEmailException::class)
    fun handleDuplicateArtistEmail(
        ex: DuplicateArtistEmailException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("DUPLICATE_ARTIST_EMAIL: {} {}", request.method, request.requestURI)
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ErrorResponse(error = ex.message ?: BackendMessages.Artist.DUPLICATE_EMAIL, code = "DUPLICATE_ARTIST_EMAIL"))
    }

    @ExceptionHandler(InvalidArtistCredentialsException::class)
    fun handleInvalidArtistCredentials(
        ex: InvalidArtistCredentialsException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("INVALID_ARTIST_CREDENTIALS: {} {}", request.method, request.requestURI)
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ErrorResponse(error = ex.message ?: BackendMessages.Artist.INVALID_CREDENTIALS, code = "INVALID_ARTIST_CREDENTIALS"))
    }

    @ExceptionHandler(InvalidArtistRefreshTokenException::class)
    fun handleInvalidArtistRefreshToken(
        ex: InvalidArtistRefreshTokenException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("INVALID_ARTIST_REFRESH_TOKEN: {} {}", request.method, request.requestURI)
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ErrorResponse(error = ex.message ?: BackendMessages.Auth.ARTIST_SESSION_EXPIRED, code = "INVALID_ARTIST_REFRESH_TOKEN"))
    }

    // Booking exceptions

    @ExceptionHandler(BookingNotFoundException::class)
    fun handleBookingNotFound(
        ex: BookingNotFoundException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("BOOKING_NOT_FOUND: {} {}", request.method, request.requestURI)
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse(error = ex.message ?: BackendMessages.Booking.NOT_FOUND, code = "BOOKING_NOT_FOUND"))
    }

    @ExceptionHandler(InvalidStatusTransitionException::class)
    fun handleInvalidTransition(
        ex: InvalidStatusTransitionException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("INVALID_STATUS_TRANSITION: {} {}", request.method, request.requestURI)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(error = ex.message ?: BackendMessages.Booking.INVALID_STATUS_TRANSITION, code = "INVALID_STATUS_TRANSITION"))
    }

    @ExceptionHandler(PaymentVerificationException::class)
    fun handlePaymentVerification(
        ex: PaymentVerificationException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("PAYMENT_VERIFICATION_FAILED: {} {}", request.method, request.requestURI)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(error = ex.message ?: BackendMessages.Booking.PAYMENT_VERIFICATION_FAILED, code = "PAYMENT_VERIFICATION_FAILED"))
    }

    @ExceptionHandler(RefundFailedException::class)
    fun handleRefundFailed(
        ex: RefundFailedException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("REFUND_FAILED: {} {} - {}", request.method, request.requestURI, ex.message, ex)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse(error = ex.message ?: BackendMessages.Booking.REFUND_FAILED, code = "REFUND_FAILED"))
    }

    @ExceptionHandler(InvalidBookingRequestException::class)
    fun handleInvalidBookingRequest(
        ex: InvalidBookingRequestException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("INVALID_BOOKING_REQUEST: {} {}", request.method, request.requestURI)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(error = ex.message ?: BackendMessages.Booking.INVALID_REQUEST, code = "INVALID_BOOKING_REQUEST"))
    }

    // Card order exceptions

    @ExceptionHandler(CardOrderNotFoundException::class)
    fun handleCardOrderNotFound(
        ex: CardOrderNotFoundException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("CARD_ORDER_NOT_FOUND: {} {}", request.method, request.requestURI)
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse(error = ex.message ?: BackendMessages.CardOrder.NOT_FOUND, code = "CARD_ORDER_NOT_FOUND"))
    }

    @ExceptionHandler(DuplicateSampleOrderException::class)
    fun handleDuplicateSampleOrder(
        ex: DuplicateSampleOrderException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("DUPLICATE_SAMPLE_ORDER: {} {}", request.method, request.requestURI)
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ErrorResponse(error = ex.message ?: BackendMessages.CardOrder.DUPLICATE_SAMPLE, code = "DUPLICATE_SAMPLE_ORDER"))
    }

    @ExceptionHandler(InvalidOrderQuantityException::class)
    fun handleInvalidOrderQuantity(
        ex: InvalidOrderQuantityException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("INVALID_ORDER_QUANTITY: {} {}", request.method, request.requestURI)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(error = ex.message ?: BackendMessages.CardOrder.INVALID_QUANTITY, code = "INVALID_ORDER_QUANTITY"))
    }

    @ExceptionHandler(InvalidOrderStatusTransitionException::class)
    fun handleInvalidOrderStatusTransition(
        ex: InvalidOrderStatusTransitionException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("INVALID_ORDER_STATUS_TRANSITION: {} {}", request.method, request.requestURI)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(error = ex.message ?: BackendMessages.CardOrder.INVALID_STATUS_TRANSITION, code = "INVALID_ORDER_STATUS_TRANSITION"))
    }

    // Card review exceptions

    @ExceptionHandler(ReviewNotEligibleException::class)
    fun handleReviewNotEligible(
        ex: ReviewNotEligibleException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("REVIEW_NOT_ELIGIBLE: {} {}", request.method, request.requestURI)
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ErrorResponse(error = ex.message ?: BackendMessages.CardReview.NOT_ELIGIBLE, code = "REVIEW_NOT_ELIGIBLE"))
    }

    @ExceptionHandler(DuplicateReviewException::class)
    fun handleDuplicateReview(
        ex: DuplicateReviewException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("DUPLICATE_REVIEW: {} {}", request.method, request.requestURI)
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ErrorResponse(error = ex.message ?: BackendMessages.CardReview.DUPLICATE, code = "DUPLICATE_REVIEW"))
    }

    @ExceptionHandler(InvalidRatingException::class)
    fun handleInvalidRating(
        ex: InvalidRatingException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("INVALID_RATING: {} {}", request.method, request.requestURI)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(error = ex.message ?: BackendMessages.CardReview.INVALID_RATING, code = "INVALID_RATING"))
    }

    // Favourite exceptions

    @ExceptionHandler(InvalidItemTypeException::class)
    fun handleInvalidItemType(
        ex: InvalidItemTypeException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("INVALID_ITEM_TYPE: {} {}", request.method, request.requestURI)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(error = ex.message ?: BackendMessages.General.INVALID_ITEM_TYPE, code = "INVALID_ITEM_TYPE"))
    }

    // Portfolio exceptions

    @ExceptionHandler(PortfolioLimitExceededException::class)
    fun handlePortfolioLimitExceeded(
        ex: PortfolioLimitExceededException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("PORTFOLIO_LIMIT_EXCEEDED: {} {}", request.method, request.requestURI)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(error = ex.message ?: BackendMessages.Upload.PORTFOLIO_LIMIT, code = "PORTFOLIO_LIMIT_EXCEEDED"))
    }

    // Pricing exceptions

    @ExceptionHandler(InvalidPricingException::class)
    fun handleInvalidPricing(
        ex: InvalidPricingException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("INVALID_PRICING: {} {}", request.method, request.requestURI)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(error = ex.message ?: BackendMessages.Validation.INVALID_PRICING, code = "INVALID_PRICING"))
    }

    // Spring / upload exceptions

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(
        ex: MethodArgumentNotValidException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val fields = ex.bindingResult.fieldErrors.joinToString(", ") { "${it.field}: ${it.defaultMessage}" }
        logger.warn("VALIDATION_ERROR: {} {} - {}", request.method, request.requestURI, fields)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(error = BackendMessages.Validation.validationFailed(fields), code = "VALIDATION_ERROR"))
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleMalformedRequest(
        ex: HttpMessageNotReadableException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("MALFORMED_REQUEST: {} {}", request.method, request.requestURI)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(error = BackendMessages.General.MALFORMED_REQUEST, code = "MALFORMED_REQUEST"))
    }

    @ExceptionHandler(MaxUploadSizeExceededException::class)
    fun handleMaxUploadSizeExceeded(
        ex: MaxUploadSizeExceededException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("FILE_TOO_LARGE: {} {}", request.method, request.requestURI)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(error = BackendMessages.Upload.FILE_TOO_LARGE, code = "FILE_TOO_LARGE"))
    }

    // Catch-all handler

    @ExceptionHandler(RateLimitExceededException::class)
    fun handleRateLimit(ex: RateLimitExceededException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
            .body(ErrorResponse(error = ex.message ?: BackendMessages.General.RATE_LIMITED, code = "RATE_LIMIT_EXCEEDED"))
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(
        ex: Exception,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("INTERNAL_ERROR: {} {} - {}", request.method, request.requestURI, ex.message, ex)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse(error = BackendMessages.General.UNEXPECTED_ERROR, code = "INTERNAL_ERROR"))
    }
}
