package com.occasi.application.dto

data class CreateCardOrderRequest(
    val cardId: Long,
    val customerId: Long,
    val quantity: Int,
    val isSample: Boolean = false,
    val deliveryAddress: String,
    val selectedSize: String = ""
)

data class CreateSampleOrderRequest(
    val cardId: Long,
    val customerId: Long,
    val deliveryAddress: String,
    val selectedSize: String = ""
)

data class UpdateOrderStatusRequest(val status: String)

data class CardOrderResponse(
    val id: Long,
    val cardId: Long,
    val cardName: String,
    val customerId: Long,
    val quantity: Int,
    val isSample: Boolean,
    val totalPrice: Int,
    val status: String,
    val deliveryAddress: String,
    val selectedSize: String,
    val orderDate: String,
    val razorpayOrderId: String?
)
