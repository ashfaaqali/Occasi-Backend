package com.occasi.application.controller

import com.occasi.application.constants.BackendRoutes
import com.occasi.application.dto.*
import com.occasi.application.model.OrderStatus
import com.occasi.application.service.CardOrderService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping(BackendRoutes.CardOrders.BASE)
class CardOrderController(private val service: CardOrderService) {

    @PostMapping
    fun createOrder(@Valid @RequestBody request: CreateCardOrderRequest): ResponseEntity<CardOrderResponse> {
        val response = if (request.isSample) {
            val sampleRequest = CreateSampleOrderRequest(
                cardId = request.cardId,
                customerId = request.customerId,
                deliveryAddress = request.deliveryAddress,
                selectedSize = request.selectedSize
            )
            service.createSampleOrder(sampleRequest)
        } else {
            service.createOrder(request)
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @PostMapping(BackendRoutes.CardOrders.VERIFY_PAYMENT)
    fun verifyPayment(
        @PathVariable orderId: Long,
        @RequestBody request: VerifyPaymentRequest
    ): ResponseEntity<CardOrderResponse> {
        val response = service.verifyPayment(orderId, request)
        return ResponseEntity.ok(response)
    }

    @PatchMapping(BackendRoutes.CardOrders.STATUS)
    fun updateStatus(
        @PathVariable orderId: Long,
        @RequestBody request: UpdateOrderStatusRequest
    ): ResponseEntity<CardOrderResponse> {
        val newStatus = OrderStatus.valueOf(request.status)
        val response = service.updateStatus(orderId, newStatus)
        return ResponseEntity.ok(response)
    }

    @GetMapping(BackendRoutes.CardOrders.BY_ID)
    fun getOrder(@PathVariable orderId: Long): ResponseEntity<CardOrderResponse> {
        val response = service.getOrder(orderId)
        return ResponseEntity.ok(response)
    }

    @GetMapping(BackendRoutes.CardOrders.BY_CUSTOMER)
    fun getCustomerOrders(@PathVariable customerId: Long): ResponseEntity<List<CardOrderResponse>> {
        val response = service.getCustomerOrders(customerId)
        return ResponseEntity.ok(response)
    }

    @GetMapping(BackendRoutes.CardOrders.SAMPLE_CHECK)
    fun hasSampleOrder(
        @RequestParam cardId: Long,
        @RequestParam customerId: Long
    ): ResponseEntity<Boolean> {
        val result = service.hasSampleOrder(cardId, customerId)
        return ResponseEntity.ok(result)
    }
}
