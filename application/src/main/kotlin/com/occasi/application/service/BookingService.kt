package com.occasi.application.service

import com.occasi.application.dto.BookingResponse
import com.occasi.application.dto.CreateBookingRequest
import com.occasi.application.exception.*
import com.occasi.application.model.*
import com.occasi.application.repository.ArtistPricingRepository
import com.occasi.application.repository.BookingRepository
import com.occasi.application.repository.HennaArtistRepository
import com.occasi.application.repository.HennaDesignRepository
import com.occasi.application.repository.UserRepository
import com.occasi.application.util.InputSanitizer
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.CacheEvict
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class BookingService(
    private val bookingRepository: BookingRepository,
    private val userRepository: UserRepository,
    private val artistRepository: HennaArtistRepository,
    private val designRepository: HennaDesignRepository,
    private val artistPricingRepository: ArtistPricingRepository,
    private val razorpayService: RazorpayService,
    private val cancellationEngine: CancellationEngine
) {

    private val logger = LoggerFactory.getLogger(BookingService::class.java)

    fun resolveBookingPrice(artistId: Long, designId: Long): Int {
        val design = designRepository.findById(designId)
            .orElseThrow { BookingNotFoundException("Design not found") }
        val complexityTier = ComplexityTier.valueOf(design.complexity.uppercase())
        val artistPricing = artistPricingRepository.findByArtistIdAndComplexity(artistId, complexityTier)
        return artistPricing?.price ?: run {
            logger.warn("No ArtistPricing found for artist $artistId, complexity $complexityTier. Falling back to design.price")
            design.price
        }
    }

    @CacheEvict(value = ["userBookings"], allEntries = true)
    fun createBooking(request: CreateBookingRequest): BookingResponse {
        // Validate required fields
        if (request.customerName.isBlank()) throw InvalidBookingRequestException("Customer name is required")
        if (request.customerPhone.isBlank()) throw InvalidBookingRequestException("Customer phone is required")
        if (request.serviceAddress.isBlank()) throw InvalidBookingRequestException("Service address is required")
        if (request.scheduledDateTime.isBlank()) throw InvalidBookingRequestException("Scheduled date/time is required")

        // Sanitize free-form text fields
        val sanitizedName = InputSanitizer.sanitize(request.customerName)
        val sanitizedAddress = InputSanitizer.sanitize(request.serviceAddress)

        // Resolve entities
        val user = userRepository.findById(request.userId)
            .orElseThrow { BookingNotFoundException("User not found") }
        val artist = artistRepository.findById(request.artistId)
            .orElseThrow { BookingNotFoundException("Artist not found") }
        val design = designRepository.findById(request.designId)
            .orElseThrow { BookingNotFoundException("Design not found") }

        val scheduledDateTime = LocalDateTime.parse(request.scheduledDateTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val paymentMethod = PaymentMethod.valueOf(request.paymentMethod)

        val resolvedPrice = resolveBookingPrice(request.artistId, request.designId)

        val booking = Booking(
            user = user,
            artist = artist,
            design = design,
            price = resolvedPrice,
            bookingStatus = if (paymentMethod == PaymentMethod.PAY_AFTER_SERVICE) BookingStatus.CONFIRMED else BookingStatus.PENDING,
            paymentStatus = if (paymentMethod == PaymentMethod.PAY_AFTER_SERVICE) PaymentStatus.PAY_AFTER_SERVICE else PaymentStatus.UNPAID,
            paymentMethod = paymentMethod,
            scheduledDateTime = scheduledDateTime,
            bookingDate = LocalDateTime.now(),
            customerName = sanitizedName,
            customerPhone = request.customerPhone,
            customerEmail = request.customerEmail ?: "",
            serviceAddress = sanitizedAddress
        )

        // Create Razorpay order for online payments
        if (paymentMethod == PaymentMethod.ONLINE) {
            try {
                val orderId = razorpayService.createOrder(resolvedPrice * 100, 0)
                booking.razorpayOrderId = orderId
            } catch (e: Exception) {
                throw RuntimeException("Failed to create payment order. Please try again or choose Pay After Service.", e)
            }
        }

        // Update design stats
        design.numberOfPeopleBooked += 1
        designRepository.save(design)

        val savedBooking = bookingRepository.save(booking)
        return toBookingResponse(savedBooking)
    }

    fun verifyPayment(bookingId: Long, paymentId: String, orderId: String, signature: String): BookingResponse {
        val booking = bookingRepository.findById(bookingId)
            .orElseThrow { BookingNotFoundException("Booking not found") }

        val isValid = razorpayService.verifySignature(orderId, paymentId, signature)
        if (!isValid) {
            throw PaymentVerificationException("Payment verification failed")
        }

        booking.bookingStatus = BookingStatus.CONFIRMED
        booking.paymentStatus = PaymentStatus.PAID
        booking.razorpayPaymentId = paymentId
        return toBookingResponse(bookingRepository.save(booking))
    }

    fun cancelBooking(bookingId: Long, reason: String, cancelledBy: CancelledBy): BookingResponse {
        val booking = bookingRepository.findById(bookingId)
            .orElseThrow { BookingNotFoundException("Booking not found") }

        if (booking.bookingStatus == BookingStatus.COMPLETED) {
            throw InvalidStatusTransitionException("Completed bookings cannot be cancelled")
        }

        val refundPercentage = cancellationEngine.calculateRefundPercentage(booking.scheduledDateTime, cancelledBy)
        val refundAmount = booking.price * refundPercentage / 100

        booking.bookingStatus = BookingStatus.CANCELLED
        booking.cancellationReason = reason
        booking.cancelledBy = cancelledBy
        booking.refundAmount = refundAmount

        // Initiate refund if booking was paid and refund amount > 0
        if (booking.paymentStatus == PaymentStatus.PAID && refundAmount > 0) {
            val refundId = razorpayService.initiateRefund(booking.razorpayPaymentId!!, refundAmount * 100)
            booking.refundId = refundId
            booking.paymentStatus = PaymentStatus.REFUND_INITIATED
        }

        return toBookingResponse(bookingRepository.save(booking))
    }

    fun updateStatus(bookingId: Long, newStatus: BookingStatus): BookingResponse {
        val booking = bookingRepository.findById(bookingId)
            .orElseThrow { BookingNotFoundException("Booking not found") }

        val currentStatus = booking.bookingStatus
        val isValidTransition = when (newStatus) {
            BookingStatus.CONFIRMED -> currentStatus == BookingStatus.PENDING
            BookingStatus.IN_PROGRESS -> currentStatus == BookingStatus.CONFIRMED
            BookingStatus.COMPLETED -> currentStatus == BookingStatus.IN_PROGRESS
            BookingStatus.CANCELLED -> currentStatus != BookingStatus.COMPLETED
            BookingStatus.PENDING -> false
        }

        if (!isValidTransition) {
            throw InvalidStatusTransitionException("Invalid status transition")
        }

        booking.bookingStatus = newStatus
        return toBookingResponse(bookingRepository.save(booking))
    }

    fun getBooking(id: Long): BookingResponse {
        val booking = bookingRepository.findById(id)
            .orElseThrow { BookingNotFoundException("Booking not found") }
        return toBookingResponse(booking)
    }

    fun getBookingsByUser(userId: Long): List<BookingResponse> {
        return bookingRepository.findByUserIdOrderByScheduledDateTimeDesc(userId)
            .map { toBookingResponse(it) }
    }

    fun getBookingsByArtist(artistId: Long): List<BookingResponse> {
        artistRepository.findById(artistId)
            .orElseThrow { ArtistNotFoundException("Artist not found") }
        return bookingRepository.findByArtistIdOrderByScheduledDateTimeDesc(artistId)
            .map { toBookingResponse(it) }
    }

    private fun toBookingResponse(booking: Booking): BookingResponse {
        return BookingResponse(
            id = booking.id!!,
            designId = booking.design.id!!,
            designName = booking.design.name,
            artistId = booking.artist.id!!,
            artistName = booking.artist.name,
            price = booking.price,
            bookingStatus = booking.bookingStatus.name,
            paymentStatus = booking.paymentStatus.name,
            paymentMethod = booking.paymentMethod.name,
            scheduledDateTime = booking.scheduledDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            customerName = booking.customerName,
            customerPhone = booking.customerPhone,
            customerEmail = booking.customerEmail,
            serviceAddress = booking.serviceAddress,
            razorpayOrderId = booking.razorpayOrderId,
            refundAmount = booking.refundAmount,
            cancellationReason = booking.cancellationReason,
            cancelledBy = booking.cancelledBy?.name,
            bookingDate = booking.bookingDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        )
    }
}
