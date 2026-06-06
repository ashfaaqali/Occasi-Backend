package com.occasi.application.service

import com.occasi.application.constants.BackendMessages
import com.occasi.application.dto.BookingResponse
import com.occasi.application.dto.CreateBookingRequest
import com.occasi.application.exception.*
import com.occasi.application.model.*
import com.occasi.application.repository.ArtistPricingRepository
import com.occasi.application.repository.ArtistPortfolioImageRepository
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
    private val portfolioImageRepository: ArtistPortfolioImageRepository,
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
        if (request.customerName.isBlank()) throw InvalidBookingRequestException(BackendMessages.Booking.CUSTOMER_NAME_REQUIRED)
        if (request.customerPhone.isBlank()) throw InvalidBookingRequestException(BackendMessages.Booking.CUSTOMER_PHONE_REQUIRED)
        if (request.serviceAddress.isBlank()) throw InvalidBookingRequestException(BackendMessages.Booking.SERVICE_ADDRESS_REQUIRED)
        if (request.scheduledDateTime.isBlank()) throw InvalidBookingRequestException(BackendMessages.Booking.SCHEDULED_DATETIME_REQUIRED)

        // Sanitize free-form text fields
        val sanitizedName = InputSanitizer.sanitize(request.customerName)
        val sanitizedAddress = InputSanitizer.sanitize(request.serviceAddress)

        // Resolve entities
        val user = userRepository.findById(request.userId)
            .orElseThrow { BookingNotFoundException("User not found") }
        val artist = artistRepository.findById(request.artistId)
            .orElseThrow { BookingNotFoundException(BackendMessages.Artist.NOT_FOUND) }
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
                throw RuntimeException(BackendMessages.Booking.PAYMENT_ORDER_FAILED, e)
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
            .orElseThrow { BookingNotFoundException(BackendMessages.Booking.NOT_FOUND) }

        val isValid = razorpayService.verifySignature(orderId, paymentId, signature)
        if (!isValid) {
            throw PaymentVerificationException(BackendMessages.Booking.PAYMENT_VERIFICATION_FAILED)
        }

        booking.bookingStatus = BookingStatus.CONFIRMED
        booking.paymentStatus = PaymentStatus.PAID
        booking.razorpayPaymentId = paymentId
        return toBookingResponse(bookingRepository.save(booking))
    }

    fun cancelBooking(bookingId: Long, reason: String, cancelledBy: CancelledBy): BookingResponse {
        val booking = bookingRepository.findById(bookingId)
            .orElseThrow { BookingNotFoundException(BackendMessages.Booking.NOT_FOUND) }

        if (booking.bookingStatus == BookingStatus.COMPLETED) {
            throw InvalidStatusTransitionException(BackendMessages.Booking.COMPLETED_CANNOT_CANCEL)
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
            .orElseThrow { BookingNotFoundException(BackendMessages.Booking.NOT_FOUND) }

        val currentStatus = booking.bookingStatus
        val isValidTransition = when (newStatus) {
            BookingStatus.CONFIRMED -> currentStatus == BookingStatus.PENDING
            BookingStatus.IN_PROGRESS -> currentStatus == BookingStatus.CONFIRMED
            BookingStatus.COMPLETED -> currentStatus == BookingStatus.IN_PROGRESS
            BookingStatus.CANCELLED -> currentStatus != BookingStatus.COMPLETED
            BookingStatus.PENDING -> false
        }

        if (!isValidTransition) {
            throw InvalidStatusTransitionException(BackendMessages.Booking.INVALID_STATUS_TRANSITION)
        }

        booking.bookingStatus = newStatus
        return toBookingResponse(bookingRepository.save(booking))
    }

    fun getBooking(id: Long): BookingResponse {
        val booking = bookingRepository.findById(id)
            .orElseThrow { BookingNotFoundException(BackendMessages.Booking.NOT_FOUND) }
        return toBookingResponse(booking)
    }

    fun getBookingsByUser(userId: Long): List<BookingResponse> {
        return bookingRepository.findByUserIdOrderByScheduledDateTimeDesc(userId)
            .map { toBookingResponse(it) }
    }

    fun getBookingsByArtist(artistId: Long): List<BookingResponse> {
        artistRepository.findById(artistId)
            .orElseThrow { ArtistNotFoundException(BackendMessages.Artist.NOT_FOUND) }
        return bookingRepository.findByArtistIdOrderByScheduledDateTimeDesc(artistId)
            .map { toBookingResponse(it) }
    }

    @CacheEvict(value = ["userBookings"], allEntries = true)
    fun updateBookingDesign(bookingId: Long, newDesignId: Long): BookingResponse {
        val booking = bookingRepository.findById(bookingId)
            .orElseThrow { BookingNotFoundException(BackendMessages.Booking.NOT_FOUND) }

        if (booking.bookingStatus != BookingStatus.CONFIRMED && booking.bookingStatus != BookingStatus.IN_PROGRESS) {
            throw InvalidStatusTransitionException("Cannot change design on a booking that is not confirmed or in progress")
        }

        val newDesign = designRepository.findById(newDesignId)
            .orElseThrow { BookingNotFoundException("Design not found") }

        val newPrice = resolveBookingPrice(booking.artist.id!!, newDesignId)
        val oldPrice = booking.price

        booking.design = newDesign

        if (newPrice < oldPrice) {
            // Cheaper design
            val difference = oldPrice - newPrice
            booking.price = newPrice
            if (booking.paymentMethod == PaymentMethod.ONLINE && booking.paymentStatus == PaymentStatus.PAID) {
                try {
                    val refundId = razorpayService.initiateRefund(booking.razorpayPaymentId!!, difference * 100)
                    booking.refundId = refundId
                    booking.refundAmount = (booking.refundAmount ?: 0) + difference
                    booking.paymentStatus = PaymentStatus.REFUND_INITIATED
                } catch (e: Exception) {
                    logger.error("Failed to process refund for cheaper design update", e)
                }
            }
        } else if (newPrice > oldPrice) {
            // More expensive design
            val difference = newPrice - oldPrice
            booking.price = newPrice
            if (booking.paymentMethod == PaymentMethod.ONLINE && booking.paymentStatus == PaymentStatus.PAID) {
                try {
                    val orderId = razorpayService.createOrder(difference * 100, bookingId)
                    booking.razorpayDiffOrderId = orderId
                    booking.paymentStatus = PaymentStatus.PENDING_DIFFERENCE
                } catch (e: Exception) {
                    throw RuntimeException("Failed to generate Razorpay order for design upgrade price difference", e)
                }
            }
        }

        return toBookingResponse(bookingRepository.save(booking))
    }

    @CacheEvict(value = ["userBookings"], allEntries = true)
    fun verifyDifferencePayment(bookingId: Long, paymentId: String, orderId: String, signature: String): BookingResponse {
        val booking = bookingRepository.findById(bookingId)
            .orElseThrow { BookingNotFoundException(BackendMessages.Booking.NOT_FOUND) }

        if (booking.paymentStatus != PaymentStatus.PENDING_DIFFERENCE) {
            throw InvalidStatusTransitionException("No pending price difference payment for this booking")
        }

        val isValid = razorpayService.verifySignature(orderId, paymentId, signature)
        if (!isValid) {
            throw PaymentVerificationException(BackendMessages.Booking.PAYMENT_VERIFICATION_FAILED)
        }

        booking.paymentStatus = PaymentStatus.PAID
        booking.razorpayDiffPaymentId = paymentId
        return toBookingResponse(bookingRepository.save(booking))
    }

    @CacheEvict(value = ["userBookings"], allEntries = true)
    fun completeBookingWithProof(bookingId: Long, imageUrl: String, addToPortfolio: Boolean): BookingResponse {
        val booking = bookingRepository.findById(bookingId)
            .orElseThrow { BookingNotFoundException(BackendMessages.Booking.NOT_FOUND) }

        if (booking.bookingStatus != BookingStatus.IN_PROGRESS && booking.bookingStatus != BookingStatus.CONFIRMED) {
            throw InvalidStatusTransitionException("Booking must be confirmed or in progress to complete")
        }

        if (booking.paymentStatus == PaymentStatus.PENDING_DIFFERENCE) {
            throw InvalidStatusTransitionException("Cannot complete booking while difference payment is pending")
        }

        booking.completedWorkImageUrl = imageUrl
        booking.bookingStatus = BookingStatus.COMPLETED

        if (addToPortfolio) {
            val portfolioImage = ArtistPortfolioImage(
                imageUrl = imageUrl
            ).apply {
                artist = booking.artist
            }
            portfolioImageRepository.save(portfolioImage)
        }

        return toBookingResponse(bookingRepository.save(booking))
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
            bookingDate = booking.bookingDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            completedWorkImageUrl = booking.completedWorkImageUrl,
            razorpayDiffOrderId = booking.razorpayDiffOrderId,
            razorpayDiffPaymentId = booking.razorpayDiffPaymentId
        )
    }
}
