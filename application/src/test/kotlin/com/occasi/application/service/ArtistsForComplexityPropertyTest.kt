package com.occasi.application.service

import com.occasi.application.config.TestFirebaseConfig
import com.occasi.application.model.ArtistPricing
import com.occasi.application.model.ComplexityTier
import com.occasi.application.model.DesignType
import com.occasi.application.model.HennaArtist
import com.occasi.application.repository.ArtistPricingRepository
import com.occasi.application.repository.HennaArtistRepository
import io.kotest.core.spec.style.StringSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.collections.shouldBeSortedWith
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

/**
 * Property tests for HennaArtistService.getArtistsForComplexity()
 * Properties 11 and 12 from the pricing-booking-overhaul design.
 */
@SpringBootTest
@Import(TestFirebaseConfig::class)
@Transactional
@OptIn(io.kotest.common.ExperimentalKotest::class)
class ArtistsForComplexityPropertyTest : StringSpec() {

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

    @Autowired
    lateinit var hennaArtistService: HennaArtistService

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

    /** Generator for a random ComplexityTier */
    private val tierArb: Arb<ComplexityTier> = arbitrary {
        ComplexityTier.entries[it.random.nextInt(0, ComplexityTier.entries.size)]
    }

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

    /** Helper: create and persist a HennaArtist */
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

        // ---------------------------------------------------------------
        // Property 11: Artists-for-Complexity Filtering
        // All returned artists have a pricing row for the queried complexity;
        // artists without one are excluded.
        // **Validates: Requirements 16.1, 16.2, 16.3**
        // ---------------------------------------------------------------
        "Property 11: getArtistsForComplexity returns only artists with pricing for the queried tier" {
            checkAll(
                PropTestConfig(minSuccess = 50),
                tierArb, priceArb, priceArb
            ) { queryTier, price1, price2 ->
                // Create artist WITH pricing for the queried tier
                val artistWith = createArtist(
                    "with-${System.nanoTime()}",
                    "with-${System.nanoTime()}@test.com",
                    "1234567890"
                )
                artistPricingRepository.saveAndFlush(
                    ArtistPricing(artist = artistWith, complexity = queryTier, designType = DesignType.HAND, price = price1)
                )

                // Create artist WITHOUT pricing for the queried tier (use a different tier)
                val otherTier = ComplexityTier.entries.first { it != queryTier }
                val artistWithout = createArtist(
                    "without-${System.nanoTime()}",
                    "without-${System.nanoTime()}@test.com",
                    "9876543210"
                )
                artistPricingRepository.saveAndFlush(
                    ArtistPricing(artist = artistWithout, complexity = otherTier, designType = DesignType.HAND, price = price2)
                )

                val results = hennaArtistService.getArtistsForComplexity(queryTier.name)

                // All returned artists must have pricing for the queried tier
                val returnedIds = results.map { it.artistId }.toSet()
                returnedIds.contains(artistWith.id!!) shouldBe true

                // Artist without pricing for the queried tier must be excluded
                returnedIds.contains(artistWithout.id!!) shouldBe false

                // Each result must include the correct price for the queried complexity
                val matchingResult = results.first { it.artistId == artistWith.id!! }
                matchingResult.priceForComplexity shouldBe price1
            }
        }

        // ---------------------------------------------------------------
        // Property 12: Artists-for-Complexity Sorting
        // Results are sorted by price ascending.
        // **Validates: Requirements 10.3, 16.4**
        // ---------------------------------------------------------------
        "Property 12: getArtistsForComplexity returns results sorted by priceForComplexity ascending" {
            checkAll(
                PropTestConfig(minSuccess = 50),
                tierArb, Arb.int(2..5)
            ) { queryTier, artistCount ->
                // Create multiple artists with different prices for the same tier
                val createdArtists = (1..artistCount).map { i ->
                    val artist = createArtist(
                        "sort-$i-${System.nanoTime()}",
                        "sort-$i-${System.nanoTime()}@test.com",
                        "1${(100000000 + i)}"
                    )
                    val price = (i * 500) + (System.nanoTime() % 100).toInt().coerceAtLeast(1)
                    artistPricingRepository.saveAndFlush(
                        ArtistPricing(artist = artist, complexity = queryTier, designType = DesignType.HAND, price = price)
                    )
                    artist to price
                }

                val results = hennaArtistService.getArtistsForComplexity(queryTier.name)

                // Results must be sorted by priceForComplexity ascending
                results.shouldBeSortedWith(compareBy { it.priceForComplexity })

                // All created artists should appear in results
                val resultIds = results.map { it.artistId }.toSet()
                createdArtists.forEach { (artist, _) ->
                    resultIds.contains(artist.id!!) shouldBe true
                }
            }
        }
    }
}
