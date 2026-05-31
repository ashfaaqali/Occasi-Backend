package com.occasi.application.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

// Requests
data class ArtistLoginRequest(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Invalid email format")
    val email: String,
    @field:NotBlank(message = "Password is required")
    val password: String
)

data class ArtistRegisterRequest(
    @field:NotBlank(message = "Name is required")
    @field:Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    val name: String,
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Invalid email format")
    val email: String,
    @field:NotBlank(message = "Mobile number is required")
    @field:Pattern(regexp = "^\\d{10}$", message = "Mobile number must be exactly 10 digits")
    val mobileNumber: String,
    @field:NotBlank(message = "Password is required")
    @field:Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    val password: String,
    val cityName: String? = null,
    val location: String? = null,
    val coverImage: String? = null,
    val pricingTiers: Map<String, Int>? = null
)

data class SendEmailOtpRequest(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Invalid email format")
    val email: String
)
data class VerifyEmailOtpRequest(val email: String, val otp: String)
data class SendPhoneOtpRequest(val phone: String)
data class VerifyPhoneOtpRequest(val phone: String, val otp: String)
data class ArtistRefreshTokenRequest(val refreshToken: String)
data class ArtistLogoutRequest(val refreshToken: String)

data class ForgotPasswordRequest(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Invalid email format")
    val email: String
)

data class ResetPasswordRequest(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Invalid email format")
    val email: String,
    @field:NotBlank(message = "OTP is required")
    val otp: String,
    @field:Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    val newPassword: String
)

// Responses
data class ArtistAuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val artist: ArtistDto
)

data class ArtistDto(
    val id: Long,
    val name: String,
    val email: String,
    val mobileNumber: String,
    val cityName: String
)
