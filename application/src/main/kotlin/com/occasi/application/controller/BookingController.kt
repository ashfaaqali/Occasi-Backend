package com.occasi.application.controller

import com.occasi.application.dto.*
import com.occasi.application.model.BookingStatus
import com.occasi.application.model.CancelledBy
import com.occasi.application.service.BookingService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/bookings")
class BookingController(private val bookingService: BookingService) {

    @PostMapping
    fun createBooking(@RequestBody request: CreateBookingRequest): ResponseEntity<BookingResponse> {
        val auth = SecurityContextHolder.getContext().authentication
        val authenticatedUserId = auth.principal as Long
        val secureRequest = request.copy(userId = authenticatedUserId)
        val response = bookingService.createBooking(secureRequest)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @PostMapping("/{id}/verify-payment")
    fun verifyPayment(
        @PathVariable id: Long,
        @RequestBody request: VerifyPaymentRequest
    ): ResponseEntity<BookingResponse> {
        val response = bookingService.verifyPayment(id, request.razorpayPaymentId, request.razorpayOrderId, request.razorpaySignature)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/{id}/cancel")
    fun cancelBooking(
        @PathVariable id: Long,
        @RequestBody request: CancelBookingRequest
    ): ResponseEntity<BookingResponse> {
        val cancelledBy = CancelledBy.valueOf(request.cancelledBy)
        val response = bookingService.cancelBooking(id, request.reason, cancelledBy)
        return ResponseEntity.ok(response)
    }

    @PutMapping("/{id}/status")
    fun updateStatus(
        @PathVariable id: Long,
        @RequestBody request: UpdateStatusRequest
    ): ResponseEntity<BookingResponse> {
        val newStatus = BookingStatus.valueOf(request.status)
        val response = bookingService.updateStatus(id, newStatus)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/{id}")
    fun getBooking(@PathVariable id: Long): ResponseEntity<BookingResponse> {
        val response = bookingService.getBooking(id)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/user/{userId}")
    fun getUserBookings(@PathVariable userId: Long): ResponseEntity<List<BookingResponse>> {
        val response = bookingService.getBookingsByUser(userId)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/artist/{artistId}")
    fun getArtistBookings(@PathVariable artistId: Long): ResponseEntity<List<BookingResponse>> {
        val response = bookingService.getBookingsByArtist(artistId)
        return ResponseEntity.ok(response)
    }
}
