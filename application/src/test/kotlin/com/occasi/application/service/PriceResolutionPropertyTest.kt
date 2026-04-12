package com.occasi.application.service

import com.occasi.application.config.TestFirebaseConfig
import com.occasi.application.model.*
import com.occasi.application.repository.ArtistPricingRepository
import com.occasi.application.repository.HennaArtistRepository
import com.occasi.application.repository.HennaDesignRepository
import io.kotest.core.spec.style.StringSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.transaction.annotation.Transactional

// Feature: pricing-booking-overhaul, Properties 1 & 2
@SpringBootTest
@Import(TestFirebaseConfig::class)
@Transactional
@OptIn(io.kotest.common.ExperimentalKotest::class)
class PriceResolutionPropertyTest : StringSpec() {

    override fun extensions() = listOf(SpringExtension)

    @TestConfiguration
    class MockGoogleAuthConfig {
        @Bean
        @Primary
        fun mockGoogleAuthService(): GoogleAuthService {
            return object : GoogleAuthService("mock-client-id") {
                override fun verifyIdToken(idToken: String): GoogleUserInfo {
                    return GoogleUserInfo(
                        email = "test@example.com",
                        name = "Test User",
                        sub = "google-sub-123"
                    )
                }
            }
        }
    }

    @Autowired
    lateinit var bookingService: BookingService

    @Autowired
    lateinit var hennaArtistRepository: HennaArtistRepository

    @Autowired
    lateinit var hennaDesignRepository: HennaDesignRepository

    @Autowired
    lateinit var artistPricingRepository: ArtistPricingRepository

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

    /** Generator for positive prices */
    private val priceArb: Arb<Int> = Arb.int(1..50000)

    /** Generator for design prices (also positive) */
    private val designPriceArb: Arb<Int> = Arb.int(1..50000)

    /** Generator for complexity strings (as stored in HennaDesign) */
    private val complexityArb: Arb<String> = Arb.element("Simple", "Mid", "Complex", "Bridal")

    /** Generator for tag strings */
    private val tagsArb: Arb<String> = Arb.element("BRIDAL", "ARABIC", "WEDDING", "FLORAL", "TRADITIONAL")

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

    private fun createDesign(name: String, price: Int, complexity: String, tags: String): HennaDesign {
        val design = HennaDesign(
            imageUrl = "http://img.png",
            name = name,
            price = price,
            complexity = complexity,
            tags = tags
        )
        return hennaDesignRepository.saveAndFlush(design)
    }

    init {

        // Property 1: Price Resolution Consistency
        // For any artist+design where ArtistPricing exists, resolved price equals ArtistPricing.price
        // **Validates: Requirements 3.1, 3.2, 9.2, 10.4, 15.1, 15.2**
        "Property 1: when ArtistPricing exists, resolveBookingPrice returns ArtistPricing.price" {
            checkAll(
                PropTestConfig(minSuccess = 100),
                nameArb, emailArb, mobileArb, nameArb, designPriceArb, complexityArb, tagsArb, priceArb
            ) { artistName, email, mobile, designName, designPrice, complexity, tags, artistPrice ->
                val artist = createArtist(artistName, email, mobile)
                val design = createDesign(designName, designPrice, complexity, tags)

                val tier = ComplexityTier.valueOf(complexity.uppercase())
                val pricing = ArtistPricing(
                    artist = artist,
                    complexity = tier,
                    price = artistPrice
                )
                artistPricingRepository.saveAndFlush(pricing)

                val resolvedPrice = bookingService.resolveBookingPrice(artist.id!!, design.id!!)

                resolvedPrice shouldBe artistPrice
            }
        }

        // Property 2: Price Fallback to Design Price
        // For any artist+design where no ArtistPricing exists, resolved price equals design.price
        // **Validates: Requirements 3.3, 10.5, 15.3, 17.1**
        "Property 2: when no ArtistPricing exists, resolveBookingPrice returns design.price" {
            checkAll(
                PropTestConfig(minSuccess = 100),
                nameArb, emailArb, mobileArb, nameArb, designPriceArb, complexityArb, tagsArb
            ) { artistName, email, mobile, designName, designPrice, complexity, tags ->
                val artist = createArtist(artistName, email, mobile)
                val design = createDesign(designName, designPrice, complexity, tags)

                // Do NOT create any ArtistPricing row

                val resolvedPrice = bookingService.resolveBookingPrice(artist.id!!, design.id!!)

                resolvedPrice shouldBe designPrice
            }
        }
    }
}
