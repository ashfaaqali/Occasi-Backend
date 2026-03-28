package com.occasi.application.dto

// Requests
data class SendOtpRequest(val phone: String)
data class VerifyOtpRequest(val phone: String, val otp: String)
data class GoogleSignInRequest(val idToken: String)
data class RefreshTokenRequest(val refreshToken: String)
data class LogoutRequest(val refreshToken: String)

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
