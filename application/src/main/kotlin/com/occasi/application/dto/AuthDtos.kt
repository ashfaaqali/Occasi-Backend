package com.occasi.application.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

// Requests
data class SendOtpRequest(
    @field:NotBlank(message = "Phone number is required")
    @field:Pattern(regexp = "^\\d{10}$", message = "Phone number must be exactly 10 digits")
    val phone: String
)

data class VerifyOtpRequest(
    @field:NotBlank(message = "Phone number is required")
    @field:Pattern(regexp = "^\\d{10}$", message = "Phone number must be exactly 10 digits")
    val phone: String,
    @field:NotBlank(message = "OTP is required")
    @field:Pattern(regexp = "^\\d{6}$", message = "OTP must be exactly 6 digits")
    val otp: String
)

data class GoogleSignInRequest(val idToken: String)

data class RefreshTokenRequest(
    @field:NotBlank(message = "Refresh token is required")
    val refreshToken: String
)

data class LogoutRequest(
    @field:NotBlank(message = "Refresh token is required")
    val refreshToken: String
)

// Responses
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val user: UserDto
)

data class TokenResponse(val accessToken: String)
data class MessageResponse(val message: String)

data class UserDto(
    val id: Long,
    val name: String,
    val email: String,
    val mobileNumber: String,
    val role: String
)
