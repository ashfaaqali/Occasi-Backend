package com.occasi.application

import com.occasi.application.dto.CreateCardOrderRequest
import com.occasi.application.dto.CreateSampleOrderRequest
import com.occasi.application.exception.DuplicateSampleOrderException
import com.occasi.application.exception.InvalidOrderQuantityException
import com.occasi.application.exception.InvalidOrderStatusTransitionException
import com.occasi.application.model.CardOrder
import com.occasi.application.model.InvitationCard
import com.occasi.application.model.OrderStatus
import com.occasi.application.repository.CardOrderRepository
import com.occasi.application.service.CardOrderService
import com.occasi.application.service.InvitationCardService
import com.occasi.application.service.RazorpayService
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.PropertyTesting
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import org.mockito.kotlin.*
import java.util.Optional

// Feature: invitation-cards-ordering
class CardOrderServicePropertyTest : StringSpec({

    beforeSpec {
        PropertyTesting.defaultIterationCount = 100
    }

    fun makeCard(
        id: Long = 1L,
        price: Int = 100,
        minOrderQuantity: Int = 10
    ) = InvitationCard(
        id = id,
        name = "Test Card",
        imageUrl = "https://example.com/img.png",
        price = price,
        finish = "MATTE",
        printType = "DIGITAL",
        size = "5×7 inches",
        material = "CARDSTOCK",
        paperWeight = 300,
        minOrderQuantity = minOrderQuantity,
        numberOfOrders = 0
    )

    // Property 11: Sample order invariants
    // **Validates: Requirements 6.1, 6.2, 9.4**
    "Property 11: sample order has quantity=1, isSample=true, totalPrice=card price" {
        val arbPrice = Arb.int(1..10000)
        val arbCardId = Arb.long(1L..1000L)
        val arbCustomerId = Arb.long(1L..1000L)

        checkAll(arbPrice, arbCardId, arbCustomerId) { price, cardId, customerId ->
            val cardOrderRepository: CardOrderRepository = mock()
            val invitationCardService: InvitationCardService = mock()
            val razorpayService: RazorpayService = mock()

            val card = makeCard(id = cardId, price = price)
            whenever(invitationCardService.getCardById(cardId)).thenReturn(card)
            whenever(cardOrderRepository.findByCardIdAndCustomerIdAndIsSampleAndStatusNot(
                eq(cardId), eq(customerId), eq(true), eq(OrderStatus.CANCELLED)
            )).thenReturn(emptyList())
            whenever(razorpayService.createOrder(any(), any())).thenReturn("order_test_123")
            doNothing().whenever(invitationCardService).incrementOrderCount(any())
            doAnswer { invocation ->
                val order = invocation.arguments[0] as CardOrder
                order.copy(id = 1L)
            }.whenever(cardOrderRepository).save(any())

            val request = CreateSampleOrderRequest(
                cardId = cardId,
                customerId = customerId,
                deliveryAddress = "123 Test St"
            )

            val response = shouldNotThrowAny { 
                CardOrderService(cardOrderRepository, invitationCardService, razorpayService)
                    .createSampleOrder(request) 
            }

            response.quantity shouldBe 1
            response.isSample shouldBe true
            response.totalPrice shouldBe price
        }
    }

    // Property 12: Duplicate sample order rejection
    // **Validates: Requirements 6.5**
    "Property 12: second sample order for same customer+card is rejected with DuplicateSampleOrderException" {
        val arbCardId = Arb.long(1L..1000L)
        val arbCustomerId = Arb.long(1L..1000L)

        checkAll(arbCardId, arbCustomerId) { cardId, customerId ->
            val cardOrderRepository: CardOrderRepository = mock()
            val invitationCardService: InvitationCardService = mock()
            val razorpayService: RazorpayService = mock()

            val card = makeCard(id = cardId, price = 100)
            whenever(invitationCardService.getCardById(cardId)).thenReturn(card)

            // An existing non-cancelled sample order already exists
            val existingOrder = CardOrder(
                id = 99L, cardId = cardId, customerId = customerId,
                quantity = 1, isSample = true, totalPrice = 100,
                status = OrderStatus.PENDING, deliveryAddress = "Existing Address"
            )
            whenever(cardOrderRepository.findByCardIdAndCustomerIdAndIsSampleAndStatusNot(
                eq(cardId), eq(customerId), eq(true), eq(OrderStatus.CANCELLED)
            )).thenReturn(listOf(existingOrder))

            val request = CreateSampleOrderRequest(
                cardId = cardId,
                customerId = customerId,
                deliveryAddress = "123 Test St"
            )

            shouldThrow<DuplicateSampleOrderException> {
                CardOrderService(cardOrderRepository, invitationCardService, razorpayService)
                    .createSampleOrder(request)
            }
        }
    }

    // Property 13: Bulk order total price calculation
    // **Validates: Requirements 7.2**
    "Property 13: for any quantity and price, totalPrice = quantity * price" {
        val arbPrice = Arb.int(1..5000)
        val arbQuantity = Arb.int(1..500)

        checkAll(arbPrice, arbQuantity) { price, quantity ->
            val cardOrderRepository: CardOrderRepository = mock()
            val invitationCardService: InvitationCardService = mock()
            val razorpayService: RazorpayService = mock()

            // Set minOrderQuantity to 1 so any quantity is valid
            val card = makeCard(price = price, minOrderQuantity = 1)
            whenever(invitationCardService.getCardById(1L)).thenReturn(card)
            whenever(razorpayService.createOrder(any(), any())).thenReturn("order_test_123")
            doNothing().whenever(invitationCardService).incrementOrderCount(any())
            doAnswer { invocation ->
                val order = invocation.arguments[0] as CardOrder
                order.copy(id = 1L)
            }.whenever(cardOrderRepository).save(any())

            val request = CreateCardOrderRequest(
                cardId = 1L,
                customerId = 1L,
                quantity = quantity,
                isSample = false,
                deliveryAddress = "123 Test St"
            )

            val response = CardOrderService(cardOrderRepository, invitationCardService, razorpayService)
                .createOrder(request)

            response.totalPrice shouldBe quantity * price
        }
    }

    // Property 15: Order initial status is PENDING
    // **Validates: Requirements 8.3**
    "Property 15: any new order has initial status PENDING" {
        val arbPrice = Arb.int(1..5000)
        val arbQuantity = Arb.int(1..500)
        val arbIsSample = Arb.boolean()

        checkAll(arbPrice, arbQuantity, arbIsSample) { price, quantity, isSample ->
            val cardOrderRepository: CardOrderRepository = mock()
            val invitationCardService: InvitationCardService = mock()
            val razorpayService: RazorpayService = mock()

            val card = makeCard(price = price, minOrderQuantity = 1)
            whenever(invitationCardService.getCardById(1L)).thenReturn(card)
            whenever(razorpayService.createOrder(any(), any())).thenReturn("order_test_123")
            doNothing().whenever(invitationCardService).incrementOrderCount(any())
            whenever(cardOrderRepository.findByCardIdAndCustomerIdAndIsSampleAndStatusNot(
                any(), any(), eq(true), eq(OrderStatus.CANCELLED)
            )).thenReturn(emptyList())
            doAnswer { invocation ->
                val order = invocation.arguments[0] as CardOrder
                order.copy(id = 1L)
            }.whenever(cardOrderRepository).save(any())

            val service = CardOrderService(cardOrderRepository, invitationCardService, razorpayService)

            val response = if (isSample) {
                service.createSampleOrder(
                    CreateSampleOrderRequest(cardId = 1L, customerId = 1L, deliveryAddress = "123 Test St")
                )
            } else {
                service.createOrder(
                    CreateCardOrderRequest(
                        cardId = 1L, customerId = 1L, quantity = quantity,
                        isSample = false, deliveryAddress = "123 Test St"
                    )
                )
            }

            response.status shouldBe "PENDING"
        }
    }

    // Property 16: Order status transition validation
    // **Validates: Requirements 8.4**
    "Property 16: status transition succeeds iff it follows the allowed graph" {
        val arbCurrentStatus = Arb.enum<OrderStatus>()
        val arbTargetStatus = Arb.enum<OrderStatus>()

        checkAll(arbCurrentStatus, arbTargetStatus) { current, target ->
            val cardOrderRepository: CardOrderRepository = mock()
            val invitationCardService: InvitationCardService = mock()
            val razorpayService: RazorpayService = mock()

            val order = CardOrder(
                id = 1L, cardId = 1L, customerId = 1L,
                quantity = 1, isSample = false, totalPrice = 100,
                status = current, deliveryAddress = "123 Test St"
            )
            whenever(cardOrderRepository.findById(1L)).thenReturn(Optional.of(order))
            doAnswer { it.arguments[0] }.whenever(cardOrderRepository).save(any())
            whenever(invitationCardService.getCardById(1L)).thenReturn(makeCard())

            // Valid transitions per the design
            val validTransitions = setOf(
                OrderStatus.PENDING to OrderStatus.CONFIRMED,
                OrderStatus.CONFIRMED to OrderStatus.PROCESSING,
                OrderStatus.PROCESSING to OrderStatus.SHIPPED,
                OrderStatus.SHIPPED to OrderStatus.DELIVERED,
                OrderStatus.PENDING to OrderStatus.CANCELLED,
                OrderStatus.CONFIRMED to OrderStatus.CANCELLED,
                OrderStatus.PROCESSING to OrderStatus.CANCELLED
            )

            val isValid = (current to target) in validTransitions

            val service = CardOrderService(cardOrderRepository, invitationCardService, razorpayService)

            if (isValid) {
                shouldNotThrowAny { service.updateStatus(1L, target) }
            } else {
                shouldThrow<InvalidOrderStatusTransitionException> {
                    service.updateStatus(1L, target)
                }
            }
        }
    }

    // Property 17: Order creation increments numberOfOrders
    // **Validates: Requirements 9.3**
    "Property 17: successful order creation increments card numberOfOrders by 1" {
        val arbIsSample = Arb.boolean()
        val arbPrice = Arb.int(1..5000)

        checkAll(arbIsSample, arbPrice) { isSample, price ->
            val cardOrderRepository: CardOrderRepository = mock()
            val invitationCardService: InvitationCardService = mock()
            val razorpayService: RazorpayService = mock()

            val card = makeCard(price = price, minOrderQuantity = 1)
            whenever(invitationCardService.getCardById(1L)).thenReturn(card)
            whenever(razorpayService.createOrder(any(), any())).thenReturn("order_test_123")
            doNothing().whenever(invitationCardService).incrementOrderCount(any())
            whenever(cardOrderRepository.findByCardIdAndCustomerIdAndIsSampleAndStatusNot(
                any(), any(), eq(true), eq(OrderStatus.CANCELLED)
            )).thenReturn(emptyList())
            doAnswer { invocation ->
                val order = invocation.arguments[0] as CardOrder
                order.copy(id = 1L)
            }.whenever(cardOrderRepository).save(any())

            val service = CardOrderService(cardOrderRepository, invitationCardService, razorpayService)

            if (isSample) {
                service.createSampleOrder(
                    CreateSampleOrderRequest(cardId = 1L, customerId = 1L, deliveryAddress = "123 Test St")
                )
            } else {
                service.createOrder(
                    CreateCardOrderRequest(
                        cardId = 1L, customerId = 1L, quantity = 5,
                        isSample = false, deliveryAddress = "123 Test St"
                    )
                )
            }

            verify(invitationCardService, times(1)).incrementOrderCount(1L)
        }
    }

    // Property 18: Non-sample order minimum quantity validation
    // **Validates: Requirements 9.5**
    "Property 18: non-sample order with quantity < minOrderQuantity is rejected" {
        val arbMinQty = Arb.int(2..100)

        checkAll(arbMinQty) { minQty ->
            val cardOrderRepository: CardOrderRepository = mock()
            val invitationCardService: InvitationCardService = mock()
            val razorpayService: RazorpayService = mock()

            val card = makeCard(minOrderQuantity = minQty)
            whenever(invitationCardService.getCardById(1L)).thenReturn(card)

            // Pick a quantity strictly less than minQty
            val quantity = minQty - 1

            val request = CreateCardOrderRequest(
                cardId = 1L,
                customerId = 1L,
                quantity = quantity,
                isSample = false,
                deliveryAddress = "123 Test St"
            )

            shouldThrow<InvalidOrderQuantityException> {
                CardOrderService(cardOrderRepository, invitationCardService, razorpayService)
                    .createOrder(request)
            }
        }
    }
})
