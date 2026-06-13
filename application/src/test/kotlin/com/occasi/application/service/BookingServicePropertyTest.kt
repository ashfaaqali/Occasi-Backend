package com.occasi.application.service

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
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import org.mockito.kotlin.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Optional

// Feature: booking-flow-payments, Properties 6-9, 11-14 for BookingService
class BookingServicePropertyTest : StringSpec({

    // --- Shared helpers ---

    fun makeUser(id: Long = 1L) = User(id = id, name = "Test User", email = "test@test.com", mobileNumber = "1234567890")

    fun makeArtist(id: Long = 1L) = HennaArtist(
        id = id, name = "Test Artist", email = "artist@test.com",
        mobileNumber = "0987654321", cityName = "Mumbai", location = "Andheri"
    )

    fun makeDesign(): HennaDesign {
        return HennaDesign(id = 1L, imageUrl = "http://img.png", name = "Bridal", complexity = "Simple", tags = "BRIDAL")
    }

    fun buildService(
        bookingRepo: BookingRepository = mock(),
        userRepo: UserRepository = mock(),
        artistRepo: HennaArtistRepository = mock(),
        designRepo: HennaDesignRepository = mock(),
        artistPricingRepo: ArtistPricingRepository = mock(),
        portfolioImageRepo: ArtistPortfolioImageRepository = mock(),
        razorpayService: RazorpayService = mock(),
        cancellationEngine: CancellationEngine = mock()
    ) = BookingService(bookingRepo, userRepo, artistRepo, designRepo, artistPricingRepo, portfolioImageRepo, razorpayService, cancellationEngine)

    // Arb generators
    val arbNonBlank = Arb.string(1..30).filter { it.isNotBlank() }
    val arbPhone = Arb.string(10..10, Codepoint.alphanumeric())
    val arbFutureDateTime = Arb.long(1L..365L).map {
        LocalDateTime.now().plusDays(it).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    }
    val arbPrice = Arb.int(100..50000)

    // **Validates: Requirements 7.2, 7.4**
    // Property 6: Booking creation persists all fields with correct defaults
    "booking creation persists all fields with correct defaults for ONLINE payment" {
        checkAll(arbNonBlank, arbPhone, arbNonBlank, arbFutureDateTime, arbPrice) { name, phone, address, dateTime, price ->
            val userRepo: UserRepository = mock()
            val artistRepo: HennaArtistRepository = mock()
            val designRepo: HennaDesignRepository = mock()
            val bookingRepo: BookingRepository = mock()
            val artistPricingRepo: ArtistPricingRepository = mock()
            val razorpayService: RazorpayService = mock()

            val user = makeUser()
            val artist = makeArtist()
            val design = makeDesign()

            whenever(userRepo.findById(1L)).thenReturn(Optional.of(user))
            whenever(artistRepo.findById(1L)).thenReturn(Optional.of(artist))
            whenever(designRepo.findById(1L)).thenReturn(Optional.of(design))
            whenever(artistPricingRepo.findByArtistIdAndComplexityAndDesignType(any(), any(), any()))
                .thenReturn(ArtistPricing(artist = artist, complexity = ComplexityTier.SIMPLE, price = price, designType = DesignType.HAND))
            whenever(razorpayService.createOrder(any(), any())).thenReturn("order_test123")
            doAnswer { it.arguments[0] }.whenever(designRepo).save(any())
            doAnswer { invocation ->
                val b = invocation.arguments[0] as Booking
                b.id = 1L
                b
            }.whenever(bookingRepo).save(any())

            val service = buildService(bookingRepo, userRepo, artistRepo, designRepo, artistPricingRepo, razorpayService = razorpayService)
            val request = CreateBookingRequest(
                userId = 1L, artistId = 1L, handDesignId = 1L, handCoverage = "FRONT",
                scheduledDateTime = dateTime, customerName = name,
                customerPhone = phone, customerEmail = "e@e.com",
                serviceAddress = address, paymentMethod = "ONLINE"
            )

            val response = service.createBooking(request)

            response.bookingStatus shouldBe "PENDING"
            response.price shouldBe price
            response.customerName shouldBe InputSanitizer.sanitize(name)
            response.customerPhone shouldBe phone
            response.serviceAddress shouldBe InputSanitizer.sanitize(address)
        }
    }

    // **Validates: Requirements 7.1**
    // Property 7: Missing required fields return 400 (InvalidBookingRequestException)
    "missing required fields throw InvalidBookingRequestException" {
        val blankFields = Arb.element("customerName", "customerPhone", "serviceAddress", "scheduledDateTime")
        checkAll(blankFields, arbNonBlank, arbPhone, arbNonBlank, arbFutureDateTime) { blankField, name, phone, address, dateTime ->
            val service = buildService()
            val request = CreateBookingRequest(
                userId = 1L, artistId = 1L, handDesignId = 1L, handCoverage = "FRONT",
                scheduledDateTime = if (blankField == "scheduledDateTime") "   " else dateTime,
                customerName = if (blankField == "customerName") "   " else name,
                customerPhone = if (blankField == "customerPhone") "   " else phone,
                customerEmail = null,
                serviceAddress = if (blankField == "serviceAddress") "   " else address,
                paymentMethod = "ONLINE"
            )

            shouldThrow<InvalidBookingRequestException> {
                service.createBooking(request)
            }
        }
    }


    // **Validates: Requirements 5.3**
    // Property 8: PAY_AFTER_SERVICE booking skips payment gateway
    "PAY_AFTER_SERVICE booking has CONFIRMED status, PAY_AFTER_SERVICE payment status, and no razorpayOrderId" {
        checkAll(arbNonBlank, arbPhone, arbNonBlank, arbFutureDateTime, arbPrice) { name, phone, address, dateTime, price ->
            val userRepo: UserRepository = mock()
            val artistRepo: HennaArtistRepository = mock()
            val designRepo: HennaDesignRepository = mock()
            val bookingRepo: BookingRepository = mock()
            val artistPricingRepo: ArtistPricingRepository = mock()
            val razorpayService: RazorpayService = mock()

            val user = makeUser()
            val artist = makeArtist()
            val design = makeDesign()

            whenever(userRepo.findById(1L)).thenReturn(Optional.of(user))
            whenever(artistRepo.findById(1L)).thenReturn(Optional.of(artist))
            whenever(designRepo.findById(1L)).thenReturn(Optional.of(design))
            whenever(artistPricingRepo.findByArtistIdAndComplexityAndDesignType(any(), any(), any()))
                .thenReturn(ArtistPricing(artist = artist, complexity = ComplexityTier.SIMPLE, price = price, designType = DesignType.HAND))
            doAnswer { it.arguments[0] }.whenever(designRepo).save(any())
            doAnswer { invocation ->
                val b = invocation.arguments[0] as Booking
                b.id = 1L
                b
            }.whenever(bookingRepo).save(any())

            val service = buildService(bookingRepo, userRepo, artistRepo, designRepo, artistPricingRepo, razorpayService = razorpayService)
            val request = CreateBookingRequest(
                userId = 1L, artistId = 1L, handDesignId = 1L, handCoverage = "FRONT",
                scheduledDateTime = dateTime, customerName = name,
                customerPhone = phone, customerEmail = null,
                serviceAddress = address, paymentMethod = "PAY_AFTER_SERVICE"
            )

            val response = service.createBooking(request)

            response.bookingStatus shouldBe "CONFIRMED"
            response.paymentStatus shouldBe "PAY_AFTER_SERVICE"
            response.razorpayOrderId.shouldBeNull()
            verify(razorpayService, never()).createOrder(any(), any())
        }
    }

    // **Validates: Requirements 5.2, 6.1**
    // Property 9: Online booking returns Razorpay order ID
    "ONLINE booking returns non-null razorpayOrderId and creates order with correct paise amount" {
        checkAll(arbNonBlank, arbPhone, arbNonBlank, arbFutureDateTime, arbPrice) { name, phone, address, dateTime, price ->
            val userRepo: UserRepository = mock()
            val artistRepo: HennaArtistRepository = mock()
            val designRepo: HennaDesignRepository = mock()
            val bookingRepo: BookingRepository = mock()
            val artistPricingRepo: ArtistPricingRepository = mock()
            val razorpayService: RazorpayService = mock()

            val user = makeUser()
            val artist = makeArtist()
            val design = makeDesign()

            whenever(userRepo.findById(1L)).thenReturn(Optional.of(user))
            whenever(artistRepo.findById(1L)).thenReturn(Optional.of(artist))
            whenever(designRepo.findById(1L)).thenReturn(Optional.of(design))
            whenever(artistPricingRepo.findByArtistIdAndComplexityAndDesignType(any(), any(), any()))
                .thenReturn(ArtistPricing(artist = artist, complexity = ComplexityTier.SIMPLE, price = price, designType = DesignType.HAND))
            whenever(razorpayService.createOrder(any(), any())).thenReturn("order_abc123")
            doAnswer { it.arguments[0] }.whenever(designRepo).save(any())
            doAnswer { invocation ->
                val b = invocation.arguments[0] as Booking
                b.id = 1L
                b
            }.whenever(bookingRepo).save(any())

            val service = buildService(bookingRepo, userRepo, artistRepo, designRepo, artistPricingRepo, razorpayService = razorpayService)
            val request = CreateBookingRequest(
                userId = 1L, artistId = 1L, handDesignId = 1L, handCoverage = "FRONT",
                scheduledDateTime = dateTime, customerName = name,
                customerPhone = phone, customerEmail = null,
                serviceAddress = address, paymentMethod = "ONLINE"
            )

            val response = service.createBooking(request)

            response.razorpayOrderId.shouldNotBeNull()
            verify(razorpayService).createOrder(eq(price * 100), any())
        }
    }


    // **Validates: Requirements 6.5, 6.6**
    // Property 11: Payment verification updates status based on signature validity
    "valid signature verification updates booking to CONFIRMED/PAID" {
        checkAll(Arb.string(5..20), Arb.string(5..20), Arb.string(5..20)) { paymentId, orderId, signature ->
            val bookingRepo: BookingRepository = mock()
            val razorpayService: RazorpayService = mock()

            val user = makeUser()
            val artist = makeArtist()
            val design = makeDesign()
            val booking = Booking(
                id = 1L, user = user, artist = artist, handDesign = design, price = 500,
                bookingStatus = BookingStatus.PENDING, paymentStatus = PaymentStatus.UNPAID,
                paymentMethod = PaymentMethod.ONLINE,
                scheduledDateTime = LocalDateTime.now().plusDays(1),
                customerName = "Test", customerPhone = "1234567890", serviceAddress = "Addr",
                razorpayOrderId = orderId
            )

            whenever(bookingRepo.findById(1L)).thenReturn(Optional.of(booking))
            whenever(razorpayService.verifySignature(orderId, paymentId, signature)).thenReturn(true)
            doAnswer { it.arguments[0] }.whenever(bookingRepo).save(any())

            val service = buildService(bookingRepo, razorpayService = razorpayService)
            val response = service.verifyPayment(1L, paymentId, orderId, signature)

            response.bookingStatus shouldBe "CONFIRMED"
            response.paymentStatus shouldBe "PAID"
        }
    }

    "invalid signature verification throws PaymentVerificationException" {
        checkAll(Arb.string(5..20), Arb.string(5..20), Arb.string(5..20)) { paymentId, orderId, signature ->
            val bookingRepo: BookingRepository = mock()
            val razorpayService: RazorpayService = mock()

            val user = makeUser()
            val artist = makeArtist()
            val design = makeDesign()
            val booking = Booking(
                id = 1L, user = user, artist = artist, handDesign = design, price = 500,
                bookingStatus = BookingStatus.PENDING, paymentStatus = PaymentStatus.UNPAID,
                paymentMethod = PaymentMethod.ONLINE,
                scheduledDateTime = LocalDateTime.now().plusDays(1),
                customerName = "Test", customerPhone = "1234567890", serviceAddress = "Addr",
                razorpayOrderId = orderId
            )

            whenever(bookingRepo.findById(1L)).thenReturn(Optional.of(booking))
            whenever(razorpayService.verifySignature(orderId, paymentId, signature)).thenReturn(false)

            val service = buildService(bookingRepo, razorpayService = razorpayService)

            shouldThrow<PaymentVerificationException> {
                service.verifyPayment(1L, paymentId, orderId, signature)
            }
        }
    }

    // **Validates: Requirements 9.5**
    // Property 12: Cancellation updates booking correctly
    "cancellation of non-COMPLETED booking sets CANCELLED with correct refund amount" {
        val cancellableStatuses = Arb.element(BookingStatus.PENDING, BookingStatus.CONFIRMED, BookingStatus.IN_PROGRESS)
        val arbReason = Arb.string(1..50).filter { it.isNotBlank() }
        val arbCancelledBy = Arb.enum<CancelledBy>()
        val arbRefundPct = Arb.element(0, 50, 100)

        checkAll(cancellableStatuses, arbReason, arbCancelledBy, arbPrice, arbRefundPct) { status, reason, cancelledBy, price, refundPct ->
            val bookingRepo: BookingRepository = mock()
            val cancellationEngine: CancellationEngine = mock()
            val razorpayService: RazorpayService = mock()

            val user = makeUser()
            val artist = makeArtist()
            val design = makeDesign()
            val booking = Booking(
                id = 1L, user = user, artist = artist, handDesign = design, price = price,
                bookingStatus = status, paymentStatus = PaymentStatus.UNPAID,
                paymentMethod = PaymentMethod.ONLINE,
                scheduledDateTime = LocalDateTime.now().plusDays(1),
                customerName = "Test", customerPhone = "1234567890", serviceAddress = "Addr"
            )

            whenever(bookingRepo.findById(1L)).thenReturn(Optional.of(booking))
            whenever(cancellationEngine.calculateRefundPercentage(any(), any())).thenReturn(refundPct)
            doAnswer { it.arguments[0] }.whenever(bookingRepo).save(any())

            val service = buildService(bookingRepo, razorpayService = razorpayService, cancellationEngine = cancellationEngine)
            val response = service.cancelBooking(1L, reason, cancelledBy)

            response.bookingStatus shouldBe "CANCELLED"
            response.cancellationReason shouldBe reason
            response.cancelledBy shouldBe cancelledBy.name
            response.refundAmount shouldBe (price * refundPct / 100)
        }
    }


    // **Validates: Requirements 9.7**
    // Property 13: PAY_AFTER_SERVICE cancellation skips refund processing
    "PAY_AFTER_SERVICE cancellation does not initiate refund" {
        val arbReason = Arb.string(1..50).filter { it.isNotBlank() }
        val arbCancelledBy = Arb.enum<CancelledBy>()

        checkAll(arbReason, arbCancelledBy, arbPrice) { reason, cancelledBy, price ->
            val bookingRepo: BookingRepository = mock()
            val cancellationEngine: CancellationEngine = mock()
            val razorpayService: RazorpayService = mock()

            val user = makeUser()
            val artist = makeArtist()
            val design = makeDesign()
            val booking = Booking(
                id = 1L, user = user, artist = artist, handDesign = design, price = price,
                bookingStatus = BookingStatus.CONFIRMED,
                paymentStatus = PaymentStatus.PAY_AFTER_SERVICE,
                paymentMethod = PaymentMethod.PAY_AFTER_SERVICE,
                scheduledDateTime = LocalDateTime.now().plusDays(1),
                customerName = "Test", customerPhone = "1234567890", serviceAddress = "Addr"
            )

            whenever(bookingRepo.findById(1L)).thenReturn(Optional.of(booking))
            whenever(cancellationEngine.calculateRefundPercentage(any(), any())).thenReturn(100)
            doAnswer { it.arguments[0] }.whenever(bookingRepo).save(any())

            val service = buildService(bookingRepo, razorpayService = razorpayService, cancellationEngine = cancellationEngine)
            val response = service.cancelBooking(1L, reason, cancelledBy)

            response.bookingStatus shouldBe "CANCELLED"
            verify(razorpayService, never()).initiateRefund(any(), any())
        }
    }

    // **Validates: Requirements 10.1**
    // Property 14: Refund initiation for PAID bookings
    "PAID booking cancellation with refund > 0 calls initiateRefund and sets REFUND_INITIATED" {
        val arbRefundPct = Arb.element(50, 100)

        checkAll(arbPrice, arbRefundPct) { price, refundPct ->
            val bookingRepo: BookingRepository = mock()
            val cancellationEngine: CancellationEngine = mock()
            val razorpayService: RazorpayService = mock()

            val user = makeUser()
            val artist = makeArtist()
            val design = makeDesign()
            val booking = Booking(
                id = 1L, user = user, artist = artist, handDesign = design, price = price,
                bookingStatus = BookingStatus.CONFIRMED,
                paymentStatus = PaymentStatus.PAID,
                paymentMethod = PaymentMethod.ONLINE,
                scheduledDateTime = LocalDateTime.now().plusDays(1),
                customerName = "Test", customerPhone = "1234567890", serviceAddress = "Addr",
                razorpayPaymentId = "pay_test123"
            )

            val expectedRefundAmount = price * refundPct / 100

            whenever(bookingRepo.findById(1L)).thenReturn(Optional.of(booking))
            whenever(cancellationEngine.calculateRefundPercentage(any(), any())).thenReturn(refundPct)
            whenever(razorpayService.initiateRefund(any(), any())).thenReturn("refund_test123")
            doAnswer { it.arguments[0] }.whenever(bookingRepo).save(any())

            val service = buildService(bookingRepo, razorpayService = razorpayService, cancellationEngine = cancellationEngine)
            val response = service.cancelBooking(1L, "Changed plans", CancelledBy.CUSTOMER)

            response.bookingStatus shouldBe "CANCELLED"
            response.paymentStatus shouldBe "REFUND_INITIATED"
            response.refundAmount shouldBe expectedRefundAmount
            verify(razorpayService).initiateRefund(eq("pay_test123"), eq(expectedRefundAmount * 100))
        }
    }
})
