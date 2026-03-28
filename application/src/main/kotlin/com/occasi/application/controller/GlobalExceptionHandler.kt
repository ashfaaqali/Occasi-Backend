package com.occasi.application.controller

import com.occasi.application.dto.MessageResponse
import com.occasi.application.service.*
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

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
}
