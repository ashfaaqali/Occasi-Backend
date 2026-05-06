package com.occasi.application.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateCardOrderRequest(
    val cardId: Long,
    val customerId: Long,
    @field:Min(value = 1, message = "Quantity must be at least 1")
    @field:Max(value = 10000, message = "Quantity must be at most 10000")
    val quantity: Int,
    val isSample: Boolean = false,
    @field:NotBlank(message = "Delivery address is required")
    @field:Size(min = 5, max = 500, message = "Address must be between 5 and 500 characters")
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
