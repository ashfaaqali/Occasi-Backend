package com.occasi.application.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
data class Booking(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne
    @JoinColumn(name = "user_id")
    var user: User,

    @ManyToOne
    @JoinColumn(name = "artist_id")
    var artist: HennaArtist,

    @ManyToOne
    @JoinColumn(name = "design_id")
    var design: HennaDesign,

    var price: Int,

    @Enumerated(EnumType.STRING)
    var bookingStatus: BookingStatus = BookingStatus.PENDING,

    @Enumerated(EnumType.STRING)
    var paymentStatus: PaymentStatus = PaymentStatus.UNPAID,

    @Enumerated(EnumType.STRING)
    var paymentMethod: PaymentMethod = PaymentMethod.ONLINE,

    var scheduledDateTime: LocalDateTime,
    var bookingDate: LocalDateTime = LocalDateTime.now(),

    // Customer details (per-booking, may differ from User record)
    var customerName: String,
    var customerPhone: String,
    var customerEmail: String = "",
    var serviceAddress: String,

    // Razorpay fields
    var razorpayOrderId: String? = null,
    var razorpayPaymentId: String? = null,

    // Cancellation/refund fields
    var refundAmount: Int? = null,
    var refundId: String? = null,
    var cancellationReason: String? = null,

    @Enumerated(EnumType.STRING)
    var cancelledBy: CancelledBy? = null
)
