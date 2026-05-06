package com.occasi.application.service

import com.occasi.application.dto.*
import com.occasi.application.exception.*
import com.occasi.application.model.CardOrder
import com.occasi.application.model.OrderStatus
import com.occasi.application.repository.CardOrderRepository
import com.occasi.application.util.InputSanitizer
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class CardOrderService(
    private val cardOrderRepository: CardOrderRepository,
    private val invitationCardService: InvitationCardService,
    private val razorpayService: RazorpayService
) {

    private val logger = LoggerFactory.getLogger(CardOrderService::class.java)

    fun createOrder(request: CreateCardOrderRequest): CardOrderResponse {
        val card = invitationCardService.getCardById(request.cardId)
            ?: throw CardOrderNotFoundException("Invitation card not found")

        if (!request.isSample && request.quantity < card.minOrderQuantity) {
            throw InvalidOrderQuantityException("Quantity must be at least ${card.minOrderQuantity}")
        }

        // Sanitize free-form text fields
        val sanitizedAddress = InputSanitizer.sanitize(request.deliveryAddress)

        val totalPrice = request.quantity * card.price

        val order = CardOrder(
            cardId = request.cardId,
            customerId = request.customerId,
            quantity = request.quantity,
            isSample = request.isSample,
            totalPrice = totalPrice,
            status = OrderStatus.PENDING,
            deliveryAddress = sanitizedAddress,
            selectedSize = request.selectedSize,
            orderDate = LocalDateTime.now()
        )

        // Save order first, then attempt Razorpay
        val savedOrder = cardOrderRepository.save(order)

        try {
            val razorpayOrderId = razorpayService.createOrder(totalPrice * 100, savedOrder.id!!)
            savedOrder.razorpayOrderId = razorpayOrderId
            cardOrderRepository.save(savedOrder)
        } catch (e: Exception) {
            logger.warn("Razorpay order creation failed for order ${savedOrder.id}: ${e.message}")
            // Order stays in PENDING without razorpayOrderId — payment can be retried
        }

        invitationCardService.incrementOrderCount(request.cardId)

        return toResponse(savedOrder, card.name)
    }

    fun createSampleOrder(request: CreateSampleOrderRequest): CardOrderResponse {
        val card = invitationCardService.getCardById(request.cardId)
            ?: throw CardOrderNotFoundException("Invitation card not found")

        // Check for duplicate non-cancelled sample order
        val existingSamples = cardOrderRepository.findByCardIdAndCustomerIdAndIsSampleAndStatusNot(
            request.cardId, request.customerId, true, OrderStatus.CANCELLED
        )
        if (existingSamples.isNotEmpty()) {
            throw DuplicateSampleOrderException("You have already ordered a sample for this card")
        }

        val order = CardOrder(
            cardId = request.cardId,
            customerId = request.customerId,
            quantity = 1,
            isSample = true,
            totalPrice = card.price,
            status = OrderStatus.PENDING,
            deliveryAddress = request.deliveryAddress,
            selectedSize = request.selectedSize,
            orderDate = LocalDateTime.now()
        )

        // Save order first, then attempt Razorpay
        val savedOrder = cardOrderRepository.save(order)

        try {
            val razorpayOrderId = razorpayService.createOrder(card.price * 100, savedOrder.id!!)
            savedOrder.razorpayOrderId = razorpayOrderId
            cardOrderRepository.save(savedOrder)
        } catch (e: Exception) {
            logger.warn("Razorpay order creation failed for sample order ${savedOrder.id}: ${e.message}")
        }

        invitationCardService.incrementOrderCount(request.cardId)

        return toResponse(savedOrder, card.name)
    }

    fun verifyPayment(orderId: Long, request: VerifyPaymentRequest): CardOrderResponse {
        val order = cardOrderRepository.findById(orderId)
            .orElseThrow { CardOrderNotFoundException("Card order not found") }

        val isValid = razorpayService.verifySignature(
            request.razorpayOrderId,
            request.razorpayPaymentId,
            request.razorpaySignature
        )
        if (!isValid) {
            throw PaymentVerificationException("Payment verification failed")
        }

        order.status = OrderStatus.CONFIRMED
        order.razorpayPaymentId = request.razorpayPaymentId

        val savedOrder = cardOrderRepository.save(order)
        val card = invitationCardService.getCardById(order.cardId)
        return toResponse(savedOrder, card?.name ?: "Unknown")
    }

    fun updateStatus(orderId: Long, newStatus: OrderStatus): CardOrderResponse {
        val order = cardOrderRepository.findById(orderId)
            .orElseThrow { CardOrderNotFoundException("Card order not found") }

        val currentStatus = order.status
        val isValidTransition = when (newStatus) {
            OrderStatus.CONFIRMED -> currentStatus == OrderStatus.PENDING
            OrderStatus.PROCESSING -> currentStatus == OrderStatus.CONFIRMED
            OrderStatus.SHIPPED -> currentStatus == OrderStatus.PROCESSING
            OrderStatus.DELIVERED -> currentStatus == OrderStatus.SHIPPED
            OrderStatus.CANCELLED -> currentStatus in listOf(
                OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.PROCESSING
            )
            OrderStatus.PENDING -> false
        }

        if (!isValidTransition) {
            throw InvalidOrderStatusTransitionException(
                "Invalid status transition from ${currentStatus.name} to ${newStatus.name}"
            )
        }

        // Handle cancellation refund logic
        if (newStatus == OrderStatus.CANCELLED) {
            if (currentStatus == OrderStatus.CONFIRMED || currentStatus == OrderStatus.PROCESSING) {
                // Full refund for CONFIRMED/PROCESSING orders
                order.razorpayPaymentId?.let { paymentId ->
                    try {
                        razorpayService.initiateRefund(paymentId, order.totalPrice * 100)
                    } catch (e: Exception) {
                        logger.error("Failed to initiate refund for order $orderId: ${e.message}", e)
                        throw RuntimeException("Failed to initiate refund. Please try again.", e)
                    }
                }
            }
            // PENDING → no refund (payment not yet made)
        }

        order.status = newStatus
        val savedOrder = cardOrderRepository.save(order)
        val card = invitationCardService.getCardById(order.cardId)
        return toResponse(savedOrder, card?.name ?: "Unknown")
    }

    fun getOrder(orderId: Long): CardOrderResponse {
        val order = cardOrderRepository.findById(orderId)
            .orElseThrow { CardOrderNotFoundException("Card order not found") }
        val card = invitationCardService.getCardById(order.cardId)
        return toResponse(order, card?.name ?: "Unknown")
    }

    fun getCustomerOrders(customerId: Long): List<CardOrderResponse> {
        return cardOrderRepository.findByCustomerIdOrderByOrderDateDesc(customerId)
            .map { order ->
                val card = invitationCardService.getCardById(order.cardId)
                toResponse(order, card?.name ?: "Unknown")
            }
    }

    fun hasSampleOrder(cardId: Long, customerId: Long): Boolean {
        val existingSamples = cardOrderRepository.findByCardIdAndCustomerIdAndIsSampleAndStatusNot(
            cardId, customerId, true, OrderStatus.CANCELLED
        )
        return existingSamples.isNotEmpty()
    }

    private fun toResponse(order: CardOrder, cardName: String): CardOrderResponse {
        return CardOrderResponse(
            id = order.id!!,
            cardId = order.cardId,
            cardName = cardName,
            customerId = order.customerId,
            quantity = order.quantity,
            isSample = order.isSample,
            totalPrice = order.totalPrice,
            status = order.status.name,
            deliveryAddress = order.deliveryAddress,
            selectedSize = order.selectedSize,
            orderDate = order.orderDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            razorpayOrderId = order.razorpayOrderId
        )
    }
}
