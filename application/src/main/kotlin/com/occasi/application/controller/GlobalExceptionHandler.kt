package com.occasi.application.controller

import com.occasi.application.dto.MessageResponse
import com.occasi.application.exception.*
import com.occasi.application.service.*
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.multipart.MaxUploadSizeExceededException

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(InvalidPhoneException::class)
    fun handleInvalidPhone(ex: InvalidPhoneException): ResponseEntity<MessageResponse> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(MessageResponse(message = ex.message ?: "Invalid phone number"))
    }

    @ExceptionHandler(InvalidOtpException::class)
    fun handleInvalidOtp(ex: InvalidOtpException): ResponseEntity<MessageResponse> {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(MessageResponse(message = ex.message ?: "Invalid OTP"))
    }

    @ExceptionHandler(OtpExpiredException::class)
    fun handleOtpExpired(ex: OtpExpiredException): ResponseEntity<MessageResponse> {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(MessageResponse(message = ex.message ?: "OTP has expired"))
    }

    @ExceptionHandler(OtpSendFailedException::class)
    fun handleOtpSendFailed(ex: OtpSendFailedException): ResponseEntity<MessageResponse> {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(MessageResponse(message = ex.message ?: "Unable to send OTP"))
    }

    @ExceptionHandler(InvalidGoogleTokenException::class)
    fun handleInvalidGoogleToken(ex: InvalidGoogleTokenException): ResponseEntity<MessageResponse> {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(MessageResponse(message = ex.message ?: "Invalid Google credentials"))
    }

    @ExceptionHandler(InvalidRefreshTokenException::class)
    fun handleInvalidRefreshToken(ex: InvalidRefreshTokenException): ResponseEntity<MessageResponse> {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(MessageResponse(message = ex.message ?: "Session expired"))
    }

    // Artist exceptions

    @ExceptionHandler(ArtistNotFoundException::class)
    fun handleArtistNotFound(ex: ArtistNotFoundException): ResponseEntity<MessageResponse> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(MessageResponse(message = ex.message ?: "Artist not found"))
    }

    @ExceptionHandler(DuplicateArtistEmailException::class)
    fun handleDuplicateArtistEmail(ex: DuplicateArtistEmailException): ResponseEntity<MessageResponse> {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(MessageResponse(message = ex.message ?: "An artist with this email already exists"))
    }

    @ExceptionHandler(InvalidArtistCredentialsException::class)
    fun handleInvalidArtistCredentials(ex: InvalidArtistCredentialsException): ResponseEntity<MessageResponse> {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(MessageResponse(message = ex.message ?: "Invalid email or password"))
    }

    @ExceptionHandler(InvalidArtistRefreshTokenException::class)
    fun handleInvalidArtistRefreshToken(ex: InvalidArtistRefreshTokenException): ResponseEntity<MessageResponse> {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(MessageResponse(message = ex.message ?: "Session expired. Please log in again."))
    }

    // Booking exceptions

    @ExceptionHandler(BookingNotFoundException::class)
    fun handleBookingNotFound(ex: BookingNotFoundException): ResponseEntity<MessageResponse> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(MessageResponse(message = ex.message ?: "Booking not found"))
    }

    @ExceptionHandler(InvalidStatusTransitionException::class)
    fun handleInvalidTransition(ex: InvalidStatusTransitionException): ResponseEntity<MessageResponse> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(MessageResponse(message = ex.message ?: "Invalid status transition"))
    }

    @ExceptionHandler(PaymentVerificationException::class)
    fun handlePaymentVerification(ex: PaymentVerificationException): ResponseEntity<MessageResponse> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(MessageResponse(message = ex.message ?: "Payment verification failed"))
    }

    @ExceptionHandler(RefundFailedException::class)
    fun handleRefundFailed(ex: RefundFailedException): ResponseEntity<MessageResponse> {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(MessageResponse(message = ex.message ?: "Failed to initiate refund"))
    }

    @ExceptionHandler(InvalidBookingRequestException::class)
    fun handleInvalidBookingRequest(ex: InvalidBookingRequestException): ResponseEntity<MessageResponse> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(MessageResponse(message = ex.message ?: "Invalid booking request"))
    }

    // Image upload exceptions

    @ExceptionHandler(MaxUploadSizeExceededException::class)
    fun handleMaxUploadSizeExceeded(ex: MaxUploadSizeExceededException): ResponseEntity<Map<String, String>> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(mapOf("error" to "File size exceeds the 5 MB limit"))
    }

    @ExceptionHandler(RuntimeException::class)
    fun handleRuntimeException(ex: RuntimeException): ResponseEntity<MessageResponse> {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(MessageResponse(message = ex.message ?: "An unexpected error occurred"))
    }
}
