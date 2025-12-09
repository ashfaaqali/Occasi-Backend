package com.occasi.application.controller

import com.occasi.application.dto.BookingRequest
import com.occasi.application.service.BookingService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/bookings")
class BookingController(private val service: BookingService) {

    @PostMapping
    fun createBooking(@RequestBody request: BookingRequest) = service.createBooking(request)

    @GetMapping("/user/{userId}")
    fun getUserBookings(@PathVariable userId: Long) = service.getBookingsByUser(userId)
}
