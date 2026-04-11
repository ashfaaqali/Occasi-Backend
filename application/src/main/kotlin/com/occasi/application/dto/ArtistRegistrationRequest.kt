package com.occasi.application.dto

data class ArtistRegistrationRequest(
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
