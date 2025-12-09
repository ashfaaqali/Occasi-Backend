package com.occasi.application.service

import com.occasi.application.dto.BookingRequest
import com.occasi.application.model.Booking
import com.occasi.application.repository.BookingRepository
import com.occasi.application.repository.HennaArtistRepository
import com.occasi.application.repository.HennaDesignRepository
import com.occasi.application.repository.UserRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class BookingService(
    private val bookingRepository: BookingRepository,
    private val userRepository: UserRepository,
    private val artistRepository: HennaArtistRepository,
    private val designRepository: HennaDesignRepository
) {
    fun createBooking(request: BookingRequest): Booking {
        val user = userRepository.findById(request.userId).orElseThrow { RuntimeException("User not found") }
        val artist = artistRepository.findById(request.artistId).orElseThrow { RuntimeException("Artist not found") }
        val design = designRepository.findById(request.designId).orElseThrow { RuntimeException("Design not found") }

        val booking = Booking(
            user = user,
            artist = artist,
            design = design,
            price = design.price,
            status = "PENDING",
            bookingDate = LocalDateTime.now()
        )
        
        // Update design stats
        design.numberOfPeopleBooked += 1
        designRepository.save(design)
        
        return bookingRepository.save(booking)
    }

    fun getBookingsByUser(userId: Long) = bookingRepository.findByUserId(userId)
}
