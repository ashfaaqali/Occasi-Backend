package com.occasi.application.dto

data class CreateBookingRequest(
    val userId: Long,
    val artistId: Long,
    val designId: Long,
    val scheduledDateTime: String,
    val customerName: String,
    val customerPhone: String,
    val customerEmail: String?,
    val serviceAddress: String,
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
