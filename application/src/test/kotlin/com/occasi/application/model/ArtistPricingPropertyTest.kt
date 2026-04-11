package com.occasi.application.model

import com.occasi.application.repository.ArtistPricingRepository
import com.occasi.application.repository.HennaArtistRepository
import com.occasi.application.service.GoogleAuthService
import com.occasi.application.service.GoogleUserInfo
import io.kotest.core.spec.style.StringSpec
import io.kotest.extensions.spring.SpringExtension
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
import org.springframework.context.annotation.Primary
import org.springframework.transaction.annotation.Transactional

// Feature: pricing-booking-overhaul, Property 5
@SpringBootTest
@Transactional
@OptIn(io.kotest.common.ExperimentalKotest::class)
class ArtistPricingPropertyTest : StringSpec() {

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
    lateinit var hennaArtistRepository: HennaArtistRepository

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

    /** Generator for non-empty subsets of ComplexityTier values */
    private val tierSubsetArb: Arb<Set<ComplexityTier>> = arbitrary {
        val allTiers = ComplexityTier.entries.toList()
        val count = it.random.nextInt(1, allTiers.size + 1) // 1 to 4 tiers
        val shuffled = allTiers.toMutableList()
        // Fisher-Yates shuffle using Kotest random
        for (i in shuffled.size - 1 downTo 1) {
            val j = it.random.nextInt(0, i + 1)
            val tmp = shuffled[i]
            shuffled[i] = shuffled[j]
            shuffled[j] = tmp
        }
        shuffled.take(count).toSet()
    }

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

    init {

        // Property 3: Price Positivity Invariant
        // For any valid ArtistPricing entity, the price value is > 0
        // **Validates: Requirements 1.4, 3.5**
        "Property 3: positive prices are accepted and the persisted price is > 0" {
            checkAll(
                PropTestConfig(minSuccess = 100),
                nameArb, emailArb, mobileArb, priceArb
            ) { name, email, mobile, price ->
                val artist = createArtist(name, email, mobile)
                val pricing = ArtistPricing(
                    artist = artist,
                    complexity = ComplexityTier.SIMPLE,
                    price = price
                )
                val saved = artistPricingRepository.saveAndFlush(pricing)
                (saved.price > 0) shouldBe true
            }
        }

        "Property 3: zero and negative prices are rejected by ArtistPricing init validation" {
            checkAll(
                PropTestConfig(minSuccess = 100),
                Arb.int(-10000..0)
            ) { invalidPrice ->
                val threw = try {
                    ArtistPricing(
                        artist = HennaArtist(
                            name = "test",
                            email = "test@test.com",
                            mobileNumber = "1234567890",
                            cityName = "City",
                            location = "Loc"
                        ),
                        complexity = ComplexityTier.SIMPLE,
                        price = invalidPrice
                    )
                    false
                } catch (e: IllegalArgumentException) {
                    true
                }
                threw shouldBe true
            }
        }

        // Property 5: Pricing Tier Uniqueness and Count
        // For any artist registered with N pricing tiers, exactly N ArtistPricing rows exist,
        // and at most one per (artist_id, complexity)
        // **Validates: Requirements 1.2, 7.4**
        "Property 5: for N pricing tiers created, exactly N rows exist with unique (artist_id, complexity) pairs" {
            checkAll(
                PropTestConfig(minSuccess = 100),
                nameArb, emailArb, mobileArb, tierSubsetArb, priceArb
            ) { name, email, mobile, tiers, basePrice ->
                // Create an artist
                val artist = createArtist(name, email, mobile)

                // Create ArtistPricing rows for the generated tier subset
                val pricingEntities = tiers.mapIndexed { index, tier ->
                    ArtistPricing(
                        artist = artist,
                        complexity = tier,
                        price = basePrice + index * 100
                    )
                }
                artistPricingRepository.saveAllAndFlush(pricingEntities)

                // Query all pricing rows for this artist
                val storedPricings = artistPricingRepository.findByArtistId(artist.id!!)

                // Assert: exactly N rows exist
                storedPricings.size shouldBe tiers.size

                // Assert: each (artist_id, complexity) pair is unique — no duplicate complexities
                val storedComplexities = storedPricings.map { it.complexity }.toSet()
                storedComplexities.size shouldBe tiers.size

                // Assert: the stored complexities match exactly the input tiers
                storedComplexities shouldBe tiers
            }
        }
    }
}
