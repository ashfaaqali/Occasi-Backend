package com.occasi.application.dto

// Requests
data class ArtistLoginRequest(val email: String, val password: String)

data class ArtistRegisterRequest(
    val name: String,
    val email: String,
    val mobileNumber: String,
    val password: String,
    val cityName: String? = null,
    val location: String? = null,
    val coverImage: String? = null,
    val portfolioImageUrls: List<String> = emptyList(),
    val pricingTiers: Map<String, Int>? = null
)

data class SendEmailOtpRequest(val email: String)
data class VerifyEmailOtpRequest(val email: String, val otp: String)
data class SendPhoneOtpRequest(val phone: String)
data class VerifyPhoneOtpRequest(val phone: String, val otp: String)
data class ArtistRefreshTokenRequest(val refreshToken: String)
data class ArtistLogoutRequest(val refreshToken: String)

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
