package com.occasi.application.controller

import com.occasi.application.dto.*
import com.occasi.application.exception.InvalidStatusTransitionException
import com.occasi.application.model.BookingStatus
import com.occasi.application.model.CancelledBy
import com.occasi.application.service.BookingService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeSortedDescendingBy
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import org.mockito.kotlin.*
import org.springframework.http.HttpStatus
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// Feature: booking-flow-payments, Properties 5, 15, 16 for BookingController
class BookingControllerTest : StringSpec({

    // --- Shared helpers ---

    fun makeBookingResponse(
        id: Long = 1L,
        handDesignId: Long? = 1L,
        handDesignName: String? = "Bridal",
        feetDesignId: Long? = null,
        feetDesignName: String? = null,
        handCoverage: String? = "FRONT",
        artistId: Long = 1L,
        price: Int = 500,
        bookingStatus: String = "PENDING",
        paymentStatus: String = "UNPAID",
        paymentMethod: String = "ONLINE",
        scheduledDateTime: String = LocalDateTime.now().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
        customerName: String = "Test",
        customerPhone: String = "1234567890",
        customerEmail: String = "test@test.com",
        serviceAddress: String = "123 Street",
        razorpayOrderId: String? = null,
        refundAmount: Int? = null,
        cancellationReason: String? = null,
        cancelledBy: String? = null,
        bookingDate: String = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    ) = BookingResponse(
        id = id, handDesignId = handDesignId, handDesignName = handDesignName,
        feetDesignId = feetDesignId, feetDesignName = feetDesignName,
        handCoverage = handCoverage,
        artistId = artistId, artistName = "Artist",
        price = price, bookingStatus = bookingStatus,
        paymentStatus = paymentStatus, paymentMethod = paymentMethod,
        scheduledDateTime = scheduledDateTime,
        customerName = customerName, customerPhone = customerPhone,
        customerEmail = customerEmail, serviceAddress = serviceAddress,
        razorpayOrderId = razorpayOrderId, refundAmount = refundAmount,
        cancellationReason = cancellationReason, cancelledBy = cancelledBy,
        bookingDate = bookingDate
    )

    // Valid transitions as defined in the design
    val validTransitions: Set<Pair<BookingStatus, BookingStatus>> = setOf(
        BookingStatus.PENDING to BookingStatus.CONFIRMED,
        BookingStatus.CONFIRMED to BookingStatus.IN_PROGRESS,
        BookingStatus.IN_PROGRESS to BookingStatus.COMPLETED,
        BookingStatus.PENDING to BookingStatus.CANCELLED,
        BookingStatus.CONFIRMED to BookingStatus.CANCELLED,
        BookingStatus.IN_PROGRESS to BookingStatus.CANCELLED,
        BookingStatus.CANCELLED to BookingStatus.CANCELLED
    )


    // **Validates: Requirements 8.1, 8.5**
    // Property 5: Booking status transitions follow lifecycle rules
    "valid status transitions are accepted by the controller" {
        val arbValidTransition = Arb.element(validTransitions.toList())

        checkAll(arbValidTransition) { (_, targetStatus) ->
            val bookingService: BookingService = mock()
            val controller = BookingController(bookingService)

            val expectedResponse = makeBookingResponse(bookingStatus = targetStatus.name)
            whenever(bookingService.updateStatus(eq(1L), eq(targetStatus))).thenReturn(expectedResponse)

            val request = UpdateStatusRequest(status = targetStatus.name)
            val response = controller.updateStatus(1L, request)

            response.statusCode shouldBe HttpStatus.OK
            response.body!!.bookingStatus shouldBe targetStatus.name
            verify(bookingService).updateStatus(1L, targetStatus)
        }
    }

    "invalid status transitions are rejected by the controller" {
        val allStatuses = BookingStatus.entries
        val allPairs = allStatuses.flatMap { from -> allStatuses.map { to -> from to to } }
        val invalidTransitions = allPairs.filter { it !in validTransitions }

        val arbInvalidTransition = Arb.element(invalidTransitions)

        checkAll(arbInvalidTransition) { (_, targetStatus) ->
            val bookingService: BookingService = mock()
            val controller = BookingController(bookingService)

            whenever(bookingService.updateStatus(eq(1L), eq(targetStatus)))
                .thenThrow(InvalidStatusTransitionException("Invalid status transition"))

            val request = UpdateStatusRequest(status = targetStatus.name)

            shouldThrow<InvalidStatusTransitionException> {
                controller.updateStatus(1L, request)
            }
        }
    }

    // **Validates: Requirements 12.1**
    // Property 15: Booking retrieval round trip
    "booking retrieval returns matching fields for any created booking" {
        val arbId = Arb.long(1L..1000L)
        val arbPrice = Arb.int(100..50000)
        val arbName = Arb.string(1..30).filter { it.isNotBlank() }
        val arbPhone = Arb.string(10..10, Codepoint.alphanumeric())
        val arbAddress = Arb.string(1..50).filter { it.isNotBlank() }
        val arbDateTime = Arb.long(1L..365L).map {
            LocalDateTime.now().plusDays(it).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        }
        val arbStatus = Arb.element("PENDING", "CONFIRMED", "IN_PROGRESS", "COMPLETED", "CANCELLED")
        val arbPaymentStatus = Arb.element("UNPAID", "PAID", "PAY_AFTER_SERVICE")

        checkAll(arbId, arbPrice, arbName, arbPhone, arbAddress, arbDateTime, arbStatus, arbPaymentStatus) {
            id, price, name, phone, address, dateTime, status, paymentStatus ->

            val bookingService: BookingService = mock()
            val controller = BookingController(bookingService)

            val expectedResponse = makeBookingResponse(
                id = id, price = price, customerName = name,
                customerPhone = phone, serviceAddress = address,
                scheduledDateTime = dateTime, bookingStatus = status,
                paymentStatus = paymentStatus
            )
            whenever(bookingService.getBooking(id)).thenReturn(expectedResponse)

            val response = controller.getBooking(id)

            response.statusCode shouldBe HttpStatus.OK
            val body = response.body!!
            body.id shouldBe id
            body.price shouldBe price
            body.customerName shouldBe name
            body.customerPhone shouldBe phone
            body.serviceAddress shouldBe address
            body.scheduledDateTime shouldBe dateTime
            body.bookingStatus shouldBe status
            body.paymentStatus shouldBe paymentStatus
        }
    }

    // **Validates: Requirements 12.2**
    // Property 16: User bookings sorted by scheduled time descending
    "getUserBookings returns bookings sorted by scheduledDateTime descending" {
        val arbUserId = Arb.long(1L..100L)
        val arbBookingCount = Arb.int(2..10)

        checkAll(arbUserId, arbBookingCount) { userId, count ->
            val bookingService: BookingService = mock()
            val controller = BookingController(bookingService)

            // Generate bookings with random future dates, then sort descending
            val baseTime = LocalDateTime.now().plusDays(1)
            val sortedDateTimes = (0 until count)
                .map { baseTime.plusHours(it.toLong() * 3 + (it.toLong() % 7)) }
                .sortedDescending()

            val bookings = sortedDateTimes.mapIndexed { index, dt ->
                makeBookingResponse(
                    id = index.toLong() + 1,
                    scheduledDateTime = dt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                )
            }

            whenever(bookingService.getBookingsByUser(userId)).thenReturn(bookings)

            val response = controller.getUserBookings(userId)

            response.statusCode shouldBe HttpStatus.OK
            val body = response.body!!
            body.size shouldBe count

            // Verify the list is sorted by scheduledDateTime descending
            val parsedDateTimes = body.map { LocalDateTime.parse(it.scheduledDateTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME) }
            parsedDateTimes.shouldBeSortedDescendingBy { it }
        }
    }
})
