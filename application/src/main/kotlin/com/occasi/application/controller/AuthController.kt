package com.occasi.application.controller

import com.occasi.application.dto.*
import com.occasi.application.service.AuthService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/auth")
class AuthController(private val authService: AuthService) {

    @PostMapping("/send-otp")
    fun sendOtp(@Valid @RequestBody request: SendOtpRequest): ResponseEntity<MessageResponse> {
        authService.sendOtp(request.phone)
        return ResponseEntity.ok(MessageResponse(message = "OTP sent"))
    }

    @PostMapping("/verify-otp")
    fun verifyOtp(@Valid @RequestBody request: VerifyOtpRequest): ResponseEntity<AuthResponse> {
        val authResponse = authService.verifyOtp(request.phone, request.otp)
        return ResponseEntity.ok(authResponse)
    }

    @PostMapping("/verify-phone")
    fun verifyPhone(@RequestBody request: VerifyOtpRequest): ResponseEntity<MessageResponse> {
        authService.verifyPhoneOnly(request.phone, request.otp)
        return ResponseEntity.ok(MessageResponse(message = "Phone verified"))
    }

    @PostMapping("/send-email-otp")
    fun sendEmailOtp(@RequestBody request: SendEmailOtpRequest): ResponseEntity<MessageResponse> {
        authService.sendEmailOtp(request.email)
        return ResponseEntity.ok(MessageResponse(message = "OTP sent to email"))
    }

    @PostMapping("/verify-email")
    fun verifyEmail(@RequestBody request: VerifyEmailOtpRequest): ResponseEntity<MessageResponse> {
        authService.verifyEmailOnly(request.email, request.otp)
        return ResponseEntity.ok(MessageResponse(message = "Email verified"))
    }

    @PostMapping("/google")
    fun googleSignIn(@RequestBody request: GoogleSignInRequest): ResponseEntity<AuthResponse> {
        val authResponse = authService.googleSignIn(request.idToken)
        return ResponseEntity.ok(authResponse)
    }

    @PostMapping("/refresh")
    fun refreshToken(@Valid @RequestBody request: RefreshTokenRequest): ResponseEntity<TokenResponse> {
        val tokenResponse = authService.refreshToken(request.refreshToken)
        return ResponseEntity.ok(tokenResponse)
    }

    @PostMapping("/logout")
    fun logout(@Valid @RequestBody request: LogoutRequest): ResponseEntity<MessageResponse> {
        authService.logout(request.refreshToken)
        return ResponseEntity.ok(MessageResponse(message = "Logged out successfully"))
    }
}
