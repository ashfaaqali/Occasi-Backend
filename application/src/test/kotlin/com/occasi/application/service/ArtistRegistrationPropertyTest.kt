package com.occasi.application.service

import com.occasi.application.config.TestFirebaseConfig
import com.occasi.application.dto.ArtistRegisterRequest
import com.occasi.application.model.ComplexityTier
import com.occasi.application.repository.ArtistPricingRepository
import com.occasi.application.repository.HennaArtistRepository
import io.kotest.core.spec.style.StringSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
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

@SpringBootTest
@Import(TestFirebaseConfig::class)
@Transactional
@OptIn(io.kotest.common.ExperimentalKotest::class)
class ArtistRegistrationPropertyTest : StringSpec() {

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
    lateinit var artistAuthService: ArtistAuthService

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

    /** Generator for valid passwords (>= 8 chars) */
    private val passwordArb: Arb<String> = arbitrary {
        val length = it.random.nextInt(8, 20)
        (1..length).map { _ -> ('a' + it.random.nextInt(0, 26)) }.joinToString("")
    }

    /** Generator for positive prices */
    private val priceArb: Arb<Int> = Arb.int(1..50000)

    /** Generator for non-empty subsets of ComplexityTier values */
    private val tierSubsetArb: Arb<Set<ComplexityTier>> = arbitrary {
        val allTiers = ComplexityTier.entries.toList()
        val count = it.random.nextInt(1, allTiers.size + 1)
        val shuffled = allTiers.toMutableList()
        for (i in shuffled.size - 1 downTo 1) {
            val j = it.random.nextInt(0, i + 1)
            val tmp = shuffled[i]
            shuffled[i] = shuffled[j]
            shuffled[j] = tmp
        }
        shuffled.take(count).toSet()
    }

    /** Generator for random cover image strings */
    private val coverImageArb: Arb<String> = arbitrary {
        val length = it.random.nextInt(10, 50)
        (1..length).map { _ -> ('a' + it.random.nextInt(0, 26)) }.joinToString("")
    }

    /** Generator for random city names */
    private val cityArb: Arb<String> = arbitrary {
        val length = it.random.nextInt(3, 15)
        (1..length).map { _ -> ('a' + it.random.nextInt(0, 26)) }.joinToString("")
    }

    /** Generator for short passwords (< 8 chars) */
    private val shortPasswordArb: Arb<String> = arbitrary {
        val length = it.random.nextInt(1, 8)
        (1..length).map { _ -> ('a' + it.random.nextInt(0, 26)) }.joinToString("")
    }

    /** Generator for zero or negative prices */
    private val invalidPriceArb: Arb<Int> = Arb.int(-10000..0)

    /** Generator for invalid tier keys (none of these uppercase to a valid ComplexityTier name) */
    private val invalidTierKeyArb: Arb<String> = arbitrary {
        val invalidKeys = listOf("EASY", "HARD", "MEDIUM", "BASIC", "PREMIUM", "ULTRA", "BEGINNER", "EXPERT")
        invalidKeys[it.random.nextInt(0, invalidKeys.size)]
    }

    /** Atomic counter for generating unique emails across iterations */
    private val emailCounter = java.util.concurrent.atomic.AtomicInteger(0)

    init {

        // Property 4: Starting Price Minimality
        // For any artist with pricing tiers, startingPrice equals min of all tier values and is <= every tier price
        // **Validates: Requirements 4.1, 4.4, 7.5**
        "Property 4: startingPrice equals min of all tier values and is <= every tier price" {
            checkAll(
                PropTestConfig(minSuccess = 50),
                nameArb, emailArb, mobileArb, passwordArb, tierSubsetArb, priceArb
            ) { name, email, mobile, password, tiers, basePrice ->
                // Build pricing tiers map with varying prices
                val pricingTiers = tiers.mapIndexed { index, tier ->
                    tier.name to (basePrice + index * 100)
                }.toMap()

                val request = ArtistRegisterRequest(
                    name = name,
                    email = email,
                    mobileNumber = mobile,
                    password = password,
                    pricingTiers = pricingTiers
                )

                val response = artistAuthService.registerArtist(request)
                val savedArtist = hennaArtistRepository.findById(response.artist.id).orElse(null)

                savedArtist shouldNotBe null

                val expectedMin = pricingTiers.values.min()
                savedArtist!!.startingPrice shouldBe expectedMin

                // startingPrice <= every tier price
                pricingTiers.values.forEach { tierPrice ->
                    (savedArtist.startingPrice <= tierPrice) shouldBe true
                }
            }
        }

        // Property 13: Registration Data Round-Trip
        // For any registration with coverImage and cityName, persisted record contains the same values
        // **Validates: Requirements 5.4, 6.3**
        "Property 13: coverImage and cityName round-trip through registration" {
            checkAll(
                PropTestConfig(minSuccess = 50),
                nameArb, emailArb, mobileArb, passwordArb, coverImageArb, cityArb
            ) { name, email, mobile, password, coverImage, city ->
                val request = ArtistRegisterRequest(
                    name = name,
                    email = email,
                    mobileNumber = mobile,
                    password = password,
                    coverImage = coverImage,
                    cityName = city
                )

                val response = artistAuthService.registerArtist(request)
                val savedArtist = hennaArtistRepository.findById(response.artist.id).orElse(null)

                savedArtist shouldNotBe null
                savedArtist!!.coverImage shouldBe coverImage
                savedArtist.cityName shouldBe city
            }
        }

        // Property 14: Registration Validation Rejects Invalid Inputs
        // Zero/negative pricing tier values, invalid tier keys, short password all rejected
        // **Validates: Requirements 7.6, 7.7, 8.2, 8.3**
        "Property 14: zero or negative pricing tier values are rejected" {
            checkAll(
                PropTestConfig(minSuccess = 50),
                nameArb, mobileArb, passwordArb, invalidPriceArb
            ) { name, mobile, password, invalidPrice ->
                val uniqueEmail = "neg${emailCounter.incrementAndGet()}@test.com"
                val pricingTiers = mapOf("SIMPLE" to invalidPrice)

                val request = ArtistRegisterRequest(
                    name = name,
                    email = uniqueEmail,
                    mobileNumber = mobile,
                    password = password,
                    pricingTiers = pricingTiers
                )

                val threw = try {
                    artistAuthService.registerArtist(request)
                    false
                } catch (e: IllegalArgumentException) {
                    true
                }
                threw shouldBe true
            }
        }

        "Property 14: invalid tier keys are rejected" {
            checkAll(
                PropTestConfig(minSuccess = 50),
                nameArb, mobileArb, passwordArb, invalidTierKeyArb, priceArb
            ) { name, mobile, password, invalidKey, price ->
                val uniqueEmail = "inv${emailCounter.incrementAndGet()}@test.com"
                val pricingTiers = mapOf(invalidKey to price)

                val request = ArtistRegisterRequest(
                    name = name,
                    email = uniqueEmail,
                    mobileNumber = mobile,
                    password = password,
                    pricingTiers = pricingTiers
                )

                val threw = try {
                    artistAuthService.registerArtist(request)
                    false
                } catch (e: IllegalArgumentException) {
                    true
                }
                threw shouldBe true
            }
        }

        "Property 14: short passwords (< 8 chars) are rejected" {
            checkAll(
                PropTestConfig(minSuccess = 50),
                nameArb, mobileArb, shortPasswordArb
            ) { name, mobile, shortPassword ->
                val uniqueEmail = "short${emailCounter.incrementAndGet()}@test.com"

                val request = ArtistRegisterRequest(
                    name = name,
                    email = uniqueEmail,
                    mobileNumber = mobile,
                    password = shortPassword
                )

                val threw = try {
                    artistAuthService.registerArtist(request)
                    false
                } catch (e: IllegalArgumentException) {
                    true
                }
                threw shouldBe true
            }
        }
    }
}
