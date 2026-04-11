package com.occasi.application.controller

import com.occasi.application.dto.*
import com.occasi.application.service.ArtistAuthService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/artist-auth")
class ArtistAuthController(private val artistAuthService: ArtistAuthService) {

    @PostMapping("/login")
    fun login(@RequestBody request: ArtistLoginRequest): ResponseEntity<ArtistAuthResponse> {
        val response = artistAuthService.login(request.email, request.password)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/register")
    fun register(@RequestBody request: ArtistRegisterRequest): ResponseEntity<ArtistAuthResponse> {
        val response = artistAuthService.registerArtist(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @PostMapping("/send-email-otp")
    fun sendEmailOtp(@RequestBody request: SendEmailOtpRequest): ResponseEntity<MessageResponse> {
        artistAuthService.sendEmailOtp(request.email)
        return ResponseEntity.ok(MessageResponse(message = "OTP sent"))
    }

    @PostMapping("/verify-email-otp")
    fun verifyEmailOtp(@RequestBody request: VerifyEmailOtpRequest): ResponseEntity<MessageResponse> {
        artistAuthService.verifyEmailOtp(request.email, request.otp)
        return ResponseEntity.ok(MessageResponse(message = "Email verified"))
    }

    @PostMapping("/send-phone-otp")
    fun sendPhoneOtp(@RequestBody request: SendPhoneOtpRequest): ResponseEntity<MessageResponse> {
        artistAuthService.sendPhoneOtp(request.phone)
        return ResponseEntity.ok(MessageResponse(message = "OTP sent"))
    }

    @PostMapping("/verify-phone-otp")
    fun verifyPhoneOtp(@RequestBody request: VerifyPhoneOtpRequest): ResponseEntity<MessageResponse> {
        artistAuthService.verifyPhoneOtp(request.phone, request.otp)
        return ResponseEntity.ok(MessageResponse(message = "Phone verified"))
    }

    @PostMapping("/refresh")
    fun refreshToken(@RequestBody request: ArtistRefreshTokenRequest): ResponseEntity<TokenResponse> {
        val response = artistAuthService.refreshToken(request.refreshToken)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/logout")
    fun logout(@RequestBody request: ArtistLogoutRequest): ResponseEntity<MessageResponse> {
        artistAuthService.logout(request.refreshToken)
        return ResponseEntity.ok(MessageResponse(message = "Logged out successfully"))
    }
}
