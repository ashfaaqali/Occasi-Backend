package com.occasi.application.dto

data class ArtistWithPriceDto(
    val artistId: Long,
    val artistName: String,
    val coverImage: String?,
    val rating: Short,
    val priceForComplexity: Int
)
