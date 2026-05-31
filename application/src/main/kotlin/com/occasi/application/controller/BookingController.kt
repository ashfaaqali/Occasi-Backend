package com.occasi.application.controller

import com.occasi.application.constants.BackendRoutes
import com.occasi.application.dto.*
import com.occasi.application.model.BookingStatus
import com.occasi.application.model.CancelledBy
import com.occasi.application.service.BookingService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping(BackendRoutes.Bookings.BASE)
class BookingController(private val bookingService: BookingService) {

    @PostMapping
    fun createBooking(@Valid @RequestBody request: CreateBookingRequest): ResponseEntity<BookingResponse> {
        val auth = SecurityContextHolder.getContext().authentication
        val authenticatedUserId = auth.principal as Long
        val secureRequest = request.copy(userId = authenticatedUserId)
        val response = bookingService.createBooking(secureRequest)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @PostMapping(BackendRoutes.Bookings.VERIFY_PAYMENT)
    fun verifyPayment(
        @PathVariable id: Long,
        @RequestBody request: VerifyPaymentRequest
    ): ResponseEntity<BookingResponse> {
        val response = bookingService.verifyPayment(id, request.razorpayPaymentId, request.razorpayOrderId, request.razorpaySignature)
        return ResponseEntity.ok(response)
    }

    @PostMapping(BackendRoutes.Bookings.CANCEL)
    fun cancelBooking(
        @PathVariable id: Long,
        @RequestBody request: CancelBookingRequest
    ): ResponseEntity<BookingResponse> {
        val cancelledBy = CancelledBy.valueOf(request.cancelledBy)
        val response = bookingService.cancelBooking(id, request.reason, cancelledBy)
        return ResponseEntity.ok(response)
    }

    @PutMapping(BackendRoutes.Bookings.STATUS)
    fun updateStatus(
        @PathVariable id: Long,
        @RequestBody request: UpdateStatusRequest
    ): ResponseEntity<BookingResponse> {
        val newStatus = BookingStatus.valueOf(request.status)
        val response = bookingService.updateStatus(id, newStatus)
        return ResponseEntity.ok(response)
    }

    @GetMapping(BackendRoutes.Bookings.BY_ID)
    fun getBooking(@PathVariable id: Long): ResponseEntity<BookingResponse> {
        val response = bookingService.getBooking(id)
        return ResponseEntity.ok(response)
    }

    @GetMapping(BackendRoutes.Bookings.BY_USER)
    fun getUserBookings(@PathVariable userId: Long): ResponseEntity<List<BookingResponse>> {
        val response = bookingService.getBookingsByUser(userId)
        return ResponseEntity.ok(response)
    }

    @GetMapping(BackendRoutes.Bookings.BY_ARTIST)
    fun getArtistBookings(@PathVariable artistId: Long): ResponseEntity<List<BookingResponse>> {
        val response = bookingService.getBookingsByArtist(artistId)
        return ResponseEntity.ok(response)
    }
}
