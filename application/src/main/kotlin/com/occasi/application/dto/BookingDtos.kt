package com.occasi.application.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class CreateBookingRequest(
    val userId: Long,
    val artistId: Long,
    val designId: Long,
    @field:NotBlank(message = "Scheduled date/time is required")
    val scheduledDateTime: String,
    @field:NotBlank(message = "Customer name is required")
    @field:Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    val customerName: String,
    @field:NotBlank(message = "Customer phone is required")
    @field:Pattern(regexp = "^\\d{10}$", message = "Phone number must be exactly 10 digits")
    val customerPhone: String,
    @field:Email(message = "Invalid email format")
    val customerEmail: String?,
    @field:NotBlank(message = "Service address is required")
    @field:Size(min = 5, max = 500, message = "Address must be between 5 and 500 characters")
    val serviceAddress: String,
    @field:NotBlank(message = "Payment method is required")
    val paymentMethod: String
)

data class VerifyPaymentRequest(
    val razorpayPaymentId: String,
    val razorpayOrderId: String,
    val razorpaySignature: String
)

data class CancelBookingRequest(
    val reason: String,
    val cancelledBy: String
)

data class UpdateStatusRequest(
    val status: String
)

data class BookingResponse(
    val id: Long,
    val designId: Long,
    val designName: String,
    val artistId: Long,
    val artistName: String,
    val price: Int,
    val bookingStatus: String,
    val paymentStatus: String,
    val paymentMethod: String,
    val scheduledDateTime: String,
    val customerName: String,
    val customerPhone: String,
    val customerEmail: String,
    val serviceAddress: String,
    val razorpayOrderId: String?,
    val refundAmount: Int?,
    val cancellationReason: String?,
    val cancelledBy: String?,
    val bookingDate: String
)
