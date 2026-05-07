package com.occasi.application.cache

import com.occasi.application.config.TestFirebaseConfig
import com.occasi.application.model.HennaArtist
import com.occasi.application.model.HennaDesign
import com.occasi.application.model.InvitationCard
import com.occasi.application.repository.HennaArtistRepository
import com.occasi.application.repository.HennaDesignRepository
import com.occasi.application.repository.InvitationCardRepository
import io.kotest.core.spec.style.StringSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

// Feature: data-caching-offline, Property 11: ETag conditional request returns 304 for unchanged data
// Feature: data-caching-offline, Property 12: If-Modified-Since returns 304 when no records changed
@SpringBootTest
@Import(TestFirebaseConfig::class)
@AutoConfigureMockMvc
@Transactional
class ConditionalRequestPropertyTest : StringSpec() {

    override fun extensions() = listOf(SpringExtension)

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var hennaArtistRepository: HennaArtistRepository

    @Autowired
    lateinit var hennaDesignRepository: HennaDesignRepository

    @Autowired
    lateinit var invitationCardRepository: InvitationCardRepository

    private val httpDateFormatter = DateTimeFormatter.RFC_1123_DATE_TIME.withZone(ZoneOffset.UTC)

    // Endpoint indices: 0 = /henna-artists, 1 = /designs, 2 = /invitation-cards
    private val arbEndpointIndex = Arb.int(0..2)

    // Only endpoints where ShallowEtagHeaderFilter is active (henna-artists, invitation-cards)
    private val arbEtagEndpointIndex = Arb.of(listOf(0, 2))

    private fun seedDataForEndpoint(endpointIndex: Int): Instant {
        val now = Instant.now()
        when (endpointIndex) {
            0 -> {
                hennaArtistRepository.save(
                    HennaArtist(
                        name = "Test Artist",
                        email = "test@example.com",
                        mobileNumber = "1234567890",
                        cityName = "Mumbai",
                        location = "Andheri",
                        updatedAt = now
                    )
                )
            }
            1 -> {
                hennaDesignRepository.save(
                    HennaDesign(
                        imageUrl = "https://example.com/design.jpg",
                        name = "Bridal Design",
                        price = 500,
                        complexity = "Simple",
                        tags = "BRIDAL",
                        updatedAt = now
                    )
                )
            }
            2 -> {
                invitationCardRepository.save(
                    InvitationCard(
                        name = "Wedding Card",
                        description = "Elegant wedding card",
                        imageUrl = "https://example.com/card.jpg",
                        price = 200,
                        finish = "MATTE",
                        printType = "DIGITAL",
                        size = "5x7 inches",
                        material = "CARDSTOCK",
                        paperWeight = 300,
                        updatedAt = now
                    )
                )
            }
        }
        return now
    }

    private fun endpointPath(index: Int): String = when (index) {
        0 -> "/henna-artists"
        1 -> "/designs"
        2 -> "/invitation-cards"
        else -> throw IllegalArgumentException("Invalid endpoint index")
    }

    init {
        // **Validates: Requirements 8.2**
        "Property 11: ETag conditional request returns 304 for unchanged data" {
            checkAll(100, arbEtagEndpointIndex) { endpointIndex ->
                seedDataForEndpoint(endpointIndex)
                val path = endpointPath(endpointIndex)

                // First GET: capture ETag
                val firstResponse = mockMvc.get(path).andReturn()
                val etag = firstResponse.response.getHeader("ETag")
                etag shouldNotBe null

                // Second GET with If-None-Match: should return 304
                mockMvc.get(path) {
                    header("If-None-Match", etag!!)
                }.andExpect {
                    status { isNotModified() }
                    content { string("") }
                }
            }
        }

        // **Validates: Requirements 8.5**
        "Property 12: If-Modified-Since returns 304 when no records changed" {
            checkAll(100, arbEndpointIndex, Arb.long(0L..60_000L)) { endpointIndex, extraMillis ->
                val lastUpdated = seedDataForEndpoint(endpointIndex)
                val path = endpointPath(endpointIndex)

                // Use If-Modified-Since with a timestamp >= the last update time
                val ifModifiedSinceInstant = lastUpdated.plusMillis(extraMillis)
                val ifModifiedSinceHeader = httpDateFormatter.format(ifModifiedSinceInstant)

                mockMvc.get(path) {
                    header("If-Modified-Since", ifModifiedSinceHeader)
                }.andExpect {
                    status { isNotModified() }
                }
            }
        }
    }
}
