package com.occasi.application.service

import com.occasi.application.config.TestFirebaseConfig
import com.occasi.application.model.*
import com.occasi.application.repository.BookingRepository
import com.occasi.application.repository.HennaArtistRepository
import com.occasi.application.repository.HennaDesignRepository
import com.occasi.application.repository.UserRepository
import io.kotest.core.spec.style.StringSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.collections.shouldBeSortedDescendingBy
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

// Feature: artist-dashboard-panel, Properties 13, 14
@SpringBootTest
@Import(TestFirebaseConfig::class)
@Transactional
class BookingServiceArtistPropertyTest : StringSpec() {

    override fun extensions() = listOf(SpringExtension)

    @TestConfiguration
    class MockGoogleAuthConfig {
        @Bean
        @Primary
        fun mockGoogleAuthService(): GoogleAuthService {
            return object : GoogleAuthService("mock-client-id") {
                override fun verifyIdToken(idToken: String): GoogleUserInfo {
                    val parts = idToken.split("|")
                    return GoogleUserInfo(
                        email = parts.getOrElse(0) { "test@example.com" },
                        name = parts.getOrElse(1) { "Test User" },
                        sub = parts.getOrElse(2) { "google-sub-123" }
                    )
                }
            }
        }
    }

    @Autowired
    lateinit var bookingService: BookingService

    @Autowired
    lateinit var bookingRepository: BookingRepository

    @Autowired
    lateinit var hennaArtistRepository: HennaArtistRepository

    @Autowired
    lateinit var hennaDesignRepository: HennaDesignRepository

    @Autowired
    lateinit var userRepository: UserRepository

    /** Generator for random names */
    private val nameArb: Arb<String> = arbitrary {
        (1..10).map { _ -> ('a' + it.random.nextInt(0, 26)) }.joinToString("")
    }

    /** Generator for random email addresses */
    private val emailArb: Arb<String> = arbitrary {
        val user = (1..8).map { _ -> ('a' + it.random.nextInt(0, 26)) }.joinToString("")
        val domain = (1..5).map { _ -> ('a' + it.random.nextInt(0, 26)) }.joinToString("")
        "$user@$domain.com"
    }

    /** Generator for random mobile numbers */
    private val mobileArb: Arb<String> = arbitrary {
        val firstDigit = it.random.nextInt(1, 10)
        val rest = (1..9).map { _ -> it.random.nextInt(0, 10) }.joinToString("")
        "$firstDigit$rest"
    }

    /** Generator for number of bookings per artist (2..5) */
    private val bookingCountArb: Arb<Int> = Arb.int(2..5)

    /**
     * Helper: create a HennaArtist and persist it.
     */
    private fun createArtist(name: String, email: String, mobile: String): HennaArtist {
        return hennaArtistRepository.saveAndFlush(
            HennaArtist(
                name = name,
                email = email,
                mobileNumber = mobile,
                cityName = "TestCity",
                location = "TestLocation"
            )
        )
    }

    /**
     * Helper: create a User and persist it.
     */
    private fun createUser(name: String, email: String, mobile: String): User {
        return userRepository.saveAndFlush(
            User(name = name, email = email, mobileNumber = mobile)
        )
    }

    /**
     * Helper: create a HennaDesign for an artist and persist it.
     */
    private fun createDesign(): HennaDesign {
        val design = HennaDesign(
            imageUrl = "http://img.png",
            name = "Design${System.nanoTime()}",
            price = 500,
            complexity = "Simple",
            tags = "BRIDAL"
        )
        return hennaDesignRepository.saveAndFlush(design)
    }

    /**
     * Helper: create a Booking and persist it.
     */
    private fun createBooking(
        user: User,
        artist: HennaArtist,
        design: HennaDesign,
        scheduledDateTime: LocalDateTime
    ): Booking {
        return bookingRepository.saveAndFlush(
            Booking(
                user = user,
                artist = artist,
                design = design,
                price = design.price,
                bookingStatus = BookingStatus.CONFIRMED,
                paymentStatus = PaymentStatus.UNPAID,
                paymentMethod = PaymentMethod.ONLINE,
                scheduledDateTime = scheduledDateTime,
                bookingDate = LocalDateTime.now(),
                customerName = user.name,
                customerPhone = user.mobileNumber,
                customerEmail = user.email,
                serviceAddress = "123 Test Street"
            )
        )
    }

    init {

        // Property 13: Artist bookings endpoint returns only that artist's bookings
        // **Validates: Requirements 7.1**
        "Property 13: getBookingsByArtist returns only bookings belonging to the queried artist" {
            checkAll(
                PropTestConfig(minSuccess = 100),
                nameArb, emailArb, mobileArb, nameArb, emailArb, mobileArb, bookingCountArb, bookingCountArb
            ) { name1, email1, mobile1, name2, email2, mobile2, count1, count2 ->
                // Create two distinct artists
                val artist1 = createArtist(name1, email1, mobile1)
                val artist2 = createArtist(name2, "other_$email2", "1$mobile2".take(10))

                // Create a shared user and designs
                val user = createUser("Customer", "cust_$email1", "9$mobile1".take(10))
                val design1 = createDesign()
                val design2 = createDesign()

                // Create bookings for artist1
                val baseTime = LocalDateTime.now().plusDays(1)
                for (i in 0 until count1) {
                    createBooking(user, artist1, design1, baseTime.plusHours(i.toLong()))
                }

                // Create bookings for artist2
                for (i in 0 until count2) {
                    createBooking(user, artist2, design2, baseTime.plusHours(i.toLong() + 10))
                }

                // Query bookings for artist1
                val artist1Bookings = bookingService.getBookingsByArtist(artist1.id!!)

                // All returned bookings must belong to artist1
                artist1Bookings.size shouldBe count1
                artist1Bookings.forEach { booking ->
                    booking.artistId shouldBe artist1.id!!
                }

                // Query bookings for artist2
                val artist2Bookings = bookingService.getBookingsByArtist(artist2.id!!)

                // All returned bookings must belong to artist2
                artist2Bookings.size shouldBe count2
                artist2Bookings.forEach { booking ->
                    booking.artistId shouldBe artist2.id!!
                }
            }
        }

        // Property 14: Artist bookings sorted by scheduledDateTime descending
        // **Validates: Requirements 7.5**
        "Property 14: getBookingsByArtist returns bookings sorted by scheduledDateTime descending" {
            checkAll(
                PropTestConfig(minSuccess = 100),
                nameArb, emailArb, mobileArb, bookingCountArb
            ) { name, email, mobile, count ->
                val artist = createArtist(name, email, mobile)
                val user = createUser("Customer", "cust_$email", "9$mobile".take(10))
                val design = createDesign()

                // Create bookings with varying scheduled times (not in order)
                val baseTime = LocalDateTime.now().plusDays(1)
                for (i in 0 until count) {
                    // Spread bookings across different times to ensure non-trivial ordering
                    val offset = ((i * 7 + 3) % (count * 5)).toLong()
                    createBooking(user, artist, design, baseTime.plusHours(offset))
                }

                val bookings = bookingService.getBookingsByArtist(artist.id!!)

                bookings.size shouldBe count

                // Verify descending order by scheduledDateTime
                val dateTimes = bookings.map { LocalDateTime.parse(it.scheduledDateTime) }
                dateTimes.shouldBeSortedDescendingBy { it }
            }
        }
    }
}
