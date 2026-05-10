package com.occasi.application.dto

data class FavouriteItemResponse(
    val itemId: Long,
    val itemType: String,
    val createdAt: String,
    val name: String,
    val imageUrl: String,
    val price: Int
)
