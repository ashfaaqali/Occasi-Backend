package com.occasi.application.service

import com.occasi.application.exception.InvalidStatusTransitionException
import com.occasi.application.model.*
import com.occasi.application.repository.ArtistPortfolioImageRepository
import com.occasi.application.repository.BookingRepository
import com.occasi.application.repository.HennaDesignRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.nulls.shouldNotBeNull
import org.mockito.kotlin.*
import java.time.LocalDateTime
import java.util.Optional

class BookingDesignChangeAndProofTest : StringSpec({

    fun makeUser() = User(id = 1L, name = "Customer", email = "customer@test.com", mobileNumber = "1234567890")
    fun makeArtist() = HennaArtist(id = 1L, name = "Artist", email = "artist@test.com", mobileNumber = "0987654321", cityName = "Delhi", location = "Noida")
    fun makeDesign(id: Long, price: Int, complexity: String = "Simple") = HennaDesign(id = id, imageUrl = "http://design.png", name = "Design $id", price = price, complexity = complexity, tags = "TAG")

    "updateBookingDesign to a cheaper design calculates difference and initiates partial refund for paid online bookings" {
        val bookingRepo: BookingRepository = mock()
        val designRepo: HennaDesignRepository = mock()
        val portfolioImageRepo: ArtistPortfolioImageRepository = mock()
        val razorpayService: RazorpayService = mock()

        val designA = makeDesign(1L, 1000)
        val designB = makeDesign(2L, 600)
        val booking = Booking(
            id = 10L, user = makeUser(), artist = makeArtist(), design = designA, price = 1000,
            bookingStatus = BookingStatus.CONFIRMED, paymentStatus = PaymentStatus.PAID,
            paymentMethod = PaymentMethod.ONLINE, scheduledDateTime = LocalDateTime.now().plusDays(1),
            customerName = "Name", customerPhone = "1234567890", serviceAddress = "Addr",
            razorpayPaymentId = "pay_original"
        )

        whenever(bookingRepo.findById(10L)).thenReturn(Optional.of(booking))
        whenever(designRepo.findById(2L)).thenReturn(Optional.of(designB))
        doAnswer { it.arguments[0] as Booking }.whenever(bookingRepo).save(any())
        whenever(razorpayService.initiateRefund(any(), any())).thenReturn("refund_123")

        val service = BookingService(
            bookingRepository = bookingRepo,
            userRepository = mock(),
            artistRepository = mock(),
            designRepository = designRepo,
            artistPricingRepository = mock(),
            portfolioImageRepository = portfolioImageRepo,
            razorpayService = razorpayService,
            cancellationEngine = mock()
        )

        val response = service.updateBookingDesign(10L, 2L)

        response.price shouldBe 600
        response.designId shouldBe 2L
        response.paymentStatus shouldBe "REFUND_INITIATED"
        response.refundAmount shouldBe 400
        verify(razorpayService).initiateRefund("pay_original", 40000)
    }

    "updateBookingDesign to a more expensive design generates a new Razorpay order for the difference" {
        val bookingRepo: BookingRepository = mock()
        val designRepo: HennaDesignRepository = mock()
        val portfolioImageRepo: ArtistPortfolioImageRepository = mock()
        val razorpayService: RazorpayService = mock()

        val designA = makeDesign(1L, 500)
        val designB = makeDesign(2L, 900)
        val booking = Booking(
            id = 10L, user = makeUser(), artist = makeArtist(), design = designA, price = 500,
            bookingStatus = BookingStatus.CONFIRMED, paymentStatus = PaymentStatus.PAID,
            paymentMethod = PaymentMethod.ONLINE, scheduledDateTime = LocalDateTime.now().plusDays(1),
            customerName = "Name", customerPhone = "1234567890", serviceAddress = "Addr",
            razorpayPaymentId = "pay_original"
        )

        whenever(bookingRepo.findById(10L)).thenReturn(Optional.of(booking))
        whenever(designRepo.findById(2L)).thenReturn(Optional.of(designB))
        doAnswer { it.arguments[0] as Booking }.whenever(bookingRepo).save(any())
        whenever(razorpayService.createOrder(any(), any())).thenReturn("order_diff_789")

        val service = BookingService(
            bookingRepository = bookingRepo,
            userRepository = mock(),
            artistRepository = mock(),
            designRepository = designRepo,
            artistPricingRepository = mock(),
            portfolioImageRepository = portfolioImageRepo,
            razorpayService = razorpayService,
            cancellationEngine = mock()
        )

        val response = service.updateBookingDesign(10L, 2L)

        response.price shouldBe 900
        response.designId shouldBe 2L
        response.paymentStatus shouldBe "PENDING_DIFFERENCE"
        response.razorpayDiffOrderId shouldBe "order_diff_789"
        verify(razorpayService).createOrder(40000, 10L)
    }

    "verifyDifferencePayment updates paymentStatus to PAID upon successful signature verification" {
        val bookingRepo: BookingRepository = mock()
        val razorpayService: RazorpayService = mock()

        val booking = Booking(
            id = 10L, user = makeUser(), artist = makeArtist(), design = makeDesign(1L, 900), price = 900,
            bookingStatus = BookingStatus.CONFIRMED, paymentStatus = PaymentStatus.PENDING_DIFFERENCE,
            paymentMethod = PaymentMethod.ONLINE, scheduledDateTime = LocalDateTime.now().plusDays(1),
            customerName = "Name", customerPhone = "1234567890", serviceAddress = "Addr",
            razorpayDiffOrderId = "order_diff_789"
        )

        whenever(bookingRepo.findById(10L)).thenReturn(Optional.of(booking))
        whenever(razorpayService.verifySignature("order_diff_789", "pay_diff_456", "sig_diff_123")).thenReturn(true)
        doAnswer { it.arguments[0] as Booking }.whenever(bookingRepo).save(any())

        val service = BookingService(
            bookingRepository = bookingRepo,
            userRepository = mock(),
            artistRepository = mock(),
            designRepository = mock(),
            artistPricingRepository = mock(),
            portfolioImageRepository = mock(),
            razorpayService = razorpayService,
            cancellationEngine = mock()
        )

        val response = service.verifyDifferencePayment(10L, "pay_diff_456", "order_diff_789", "sig_diff_123")

        response.paymentStatus shouldBe "PAID"
        response.razorpayDiffPaymentId shouldBe "pay_diff_456"
    }

    "completeBookingWithProof saves completedWorkImageUrl and transitions booking to COMPLETED, adding to portfolio if requested" {
        val bookingRepo: BookingRepository = mock()
        val portfolioImageRepo: ArtistPortfolioImageRepository = mock()

        val artist = makeArtist()
        val booking = Booking(
            id = 10L, user = makeUser(), artist = artist, design = makeDesign(1L, 500), price = 500,
            bookingStatus = BookingStatus.IN_PROGRESS, paymentStatus = PaymentStatus.PAID,
            paymentMethod = PaymentMethod.ONLINE, scheduledDateTime = LocalDateTime.now().plusDays(1),
            customerName = "Name", customerPhone = "1234567890", serviceAddress = "Addr"
        )

        whenever(bookingRepo.findById(10L)).thenReturn(Optional.of(booking))
        doAnswer { it.arguments[0] as Booking }.whenever(bookingRepo).save(any())

        val service = BookingService(
            bookingRepository = bookingRepo,
            userRepository = mock(),
            artistRepository = mock(),
            designRepository = mock(),
            artistPricingRepository = mock(),
            portfolioImageRepository = portfolioImageRepo,
            razorpayService = mock(),
            cancellationEngine = mock()
        )

        val response = service.completeBookingWithProof(10L, "http://finished-henna.png", addToPortfolio = true)

        response.bookingStatus shouldBe "COMPLETED"
        response.completedWorkImageUrl shouldBe "http://finished-henna.png"

        argumentCaptor<ArtistPortfolioImage>().apply {
            verify(portfolioImageRepo).save(capture())
            firstValue.imageUrl shouldBe "http://finished-henna.png"
            firstValue.artist shouldBe artist
        }
    }

    "completeBookingWithProof throws exception if a difference payment is pending" {
        val bookingRepo: BookingRepository = mock()

        val booking = Booking(
            id = 10L, user = makeUser(), artist = makeArtist(), design = makeDesign(1L, 900), price = 900,
            bookingStatus = BookingStatus.IN_PROGRESS, paymentStatus = PaymentStatus.PENDING_DIFFERENCE,
            paymentMethod = PaymentMethod.ONLINE, scheduledDateTime = LocalDateTime.now().plusDays(1),
            customerName = "Name", customerPhone = "1234567890", serviceAddress = "Addr"
        )

        whenever(bookingRepo.findById(10L)).thenReturn(Optional.of(booking))

        val service = BookingService(
            bookingRepository = bookingRepo,
            userRepository = mock(),
            artistRepository = mock(),
            designRepository = mock(),
            artistPricingRepository = mock(),
            portfolioImageRepository = mock(),
            razorpayService = mock(),
            cancellationEngine = mock()
        )

        shouldThrow<InvalidStatusTransitionException> {
            service.completeBookingWithProof(10L, "http://finished-henna.png", addToPortfolio = false)
        }
    }
})
