package com.occasi.application.dto

data class BookingRequest(
    val userId: Long,
    val artistId: Long,
    val designId: Long
)
