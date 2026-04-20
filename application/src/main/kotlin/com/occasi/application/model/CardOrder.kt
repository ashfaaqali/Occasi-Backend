package com.occasi.application.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
data class CardOrder(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    var cardId: Long,
    var customerId: Long,
    var quantity: Int,
    var isSample: Boolean = false,
    var totalPrice: Int,
    @Enumerated(EnumType.STRING)
    var status: OrderStatus = OrderStatus.PENDING,
    var deliveryAddress: String,
    var selectedSize: String = "",
    var orderDate: LocalDateTime = LocalDateTime.now(),
    var razorpayOrderId: String? = null,
    var razorpayPaymentId: String? = null
)

enum class OrderStatus {
    PENDING, CONFIRMED, PROCESSING, SHIPPED, DELIVERED, CANCELLED
}
