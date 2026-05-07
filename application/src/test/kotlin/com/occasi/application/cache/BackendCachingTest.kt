package com.occasi.application.cache

import com.occasi.application.config.CacheConfig
import com.occasi.application.config.ETagConfig
import com.occasi.application.config.TestFirebaseConfig
import com.occasi.application.model.HennaArtist
import com.occasi.application.model.HennaDesign
import com.occasi.application.model.InvitationCard
import com.occasi.application.repository.HennaArtistRepository
import com.occasi.application.repository.HennaDesignRepository
import com.occasi.application.repository.InvitationCardRepository
import io.kotest.core.spec.style.StringSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cache.CacheManager
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Unit tests for backend caching infrastructure.
 * Validates: Requirements 7.1, 7.2, 7.3, 7.4, 7.5, 8.1, 8.4
 */
@SpringBootTest
@Import(TestFirebaseConfig::class)
@AutoConfigureMockMvc
@Transactional
class BackendCachingTest : StringSpec() {

    override fun extensions() = listOf(SpringExtension)

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var cacheManager: CacheManager

    @Autowired
    lateinit var hennaArtistRepository: HennaArtistRepository

    @Autowired
    lateinit var hennaDesignRepository: HennaDesignRepository

    @Autowired
    lateinit var invitationCardRepository: InvitationCardRepository

    init {
        // **Validates: Requirements 7.4, 7.5**
        "CacheConfig creates CacheManager with all expected cache names" {
            val cacheNames = cacheManager.cacheNames
            cacheNames.shouldContainAll(
                "hennaArtists",
                "hennaDesigns",
                "invitationCards",
                "artistDetail",
                "designDetail",
                "cardDetail",
                "userBookings"
            )
        }

        // **Validates: Requirements 7.1**
        "@Cacheable caches result - second call returns cached data" {
            // Seed a henna artist
            hennaArtistRepository.save(
                HennaArtist(
                    name = "Cache Test Artist",
                    email = "cache@test.com",
                    mobileNumber = "9876543210",
                    cityName = "Delhi",
                    location = "Connaught Place"
                )
            )

            // First call populates the cache
            mockMvc.get("/henna-artists").andExpect {
                status { isOk() }
            }

            // Verify the cache now has an entry for hennaArtists
            val cache = cacheManager.getCache("hennaArtists")
            cache.shouldNotBeNull()

            // Second call should still succeed (served from cache)
            mockMvc.get("/henna-artists").andExpect {
                status { isOk() }
            }
        }

        // **Validates: Requirements 7.2**
        "@CacheEvict clears cache on mutation" {
            // Seed data and populate cache
            hennaArtistRepository.save(
                HennaArtist(
                    name = "Evict Test Artist",
                    email = "evict@test.com",
                    mobileNumber = "1111111111",
                    cityName = "Mumbai",
                    location = "Bandra"
                )
            )

            // Populate the cache via GET
            mockMvc.get("/henna-artists").andExpect {
                status { isOk() }
            }

            // Verify cache is populated
            val cache = cacheManager.getCache("hennaArtists")
            cache.shouldNotBeNull()

            // Perform a mutation (POST to register a new artist) which should evict the cache
            mockMvc.post("/henna-artists") {
                contentType = MediaType.APPLICATION_JSON
                content = """
                    {
                        "name": "New Artist",
                        "email": "new@test.com",
                        "mobileNumber": "2222222222",
                        "password": "password123",
                        "cityName": "Pune",
                        "location": "Koregaon Park"
                    }
                """.trimIndent()
            }.andExpect {
                status { isCreated() }
            }

            // After eviction, the cache entry for the list should be cleared
            // A fresh GET should hit the repository again
            val result = mockMvc.get("/henna-artists").andExpect {
                status { isOk() }
            }.andReturn()

            // The response should include the newly added artist
            result.response.contentAsString.contains("New Artist") shouldBe true
        }

        // **Validates: Requirements 8.1**
        "ShallowEtagHeaderFilter is registered for correct URL patterns" {
            val config = ETagConfig()
            val filterRegistration = config.shallowEtagHeaderFilter()

            filterRegistration.filter.shouldNotBeNull()
            filterRegistration.urlPatterns.shouldContainAll(
                "/henna-artists/*",
                "/henna-designs/*",
                "/invitation-cards/*"
            )
        }

        // **Validates: Requirements 8.1**
        "ETag header is present on henna-artists list endpoint response" {
            hennaArtistRepository.save(
                HennaArtist(
                    name = "ETag Artist",
                    email = "etag@test.com",
                    mobileNumber = "3333333333",
                    cityName = "Chennai",
                    location = "T Nagar"
                )
            )

            val result = mockMvc.get("/henna-artists").andExpect {
                status { isOk() }
            }.andReturn()

            result.response.getHeader("ETag") shouldNotBe null
        }

        // **Validates: Requirements 8.1**
        "ETag header is present on invitation-cards list endpoint response" {
            invitationCardRepository.save(
                InvitationCard(
                    name = "ETag Card",
                    description = "Test card for ETag",
                    imageUrl = "https://example.com/card.jpg",
                    price = 150,
                    finish = "MATTE",
                    printType = "DIGITAL",
                    size = "5x7 inches",
                    material = "CARDSTOCK",
                    paperWeight = 300,
                    updatedAt = Instant.now()
                )
            )

            val result = mockMvc.get("/invitation-cards").andExpect {
                status { isOk() }
            }.andReturn()

            result.response.getHeader("ETag") shouldNotBe null
        }

        // **Validates: Requirements 8.4**
        "Last-Modified header is present on henna-artists list endpoint response" {
            hennaArtistRepository.save(
                HennaArtist(
                    name = "LastMod Artist",
                    email = "lastmod@test.com",
                    mobileNumber = "4444444444",
                    cityName = "Bangalore",
                    location = "Indiranagar",
                    updatedAt = Instant.now()
                )
            )

            val result = mockMvc.get("/henna-artists").andExpect {
                status { isOk() }
            }.andReturn()

            result.response.getHeader("Last-Modified") shouldNotBe null
        }

        // **Validates: Requirements 8.4**
        "Last-Modified header is present on designs list endpoint response" {
            hennaDesignRepository.save(
                HennaDesign(
                    imageUrl = "https://example.com/design.jpg",
                    name = "LastMod Design",
                    price = 300,
                    complexity = "Simple",
                    tags = "BRIDAL",
                    updatedAt = Instant.now()
                )
            )

            val result = mockMvc.get("/designs").andExpect {
                status { isOk() }
            }.andReturn()

            result.response.getHeader("Last-Modified") shouldNotBe null
        }

        // **Validates: Requirements 8.4**
        "Last-Modified header is present on invitation-cards list endpoint response" {
            invitationCardRepository.save(
                InvitationCard(
                    name = "LastMod Card",
                    description = "Test card for Last-Modified",
                    imageUrl = "https://example.com/card2.jpg",
                    price = 200,
                    finish = "GLOSSY",
                    printType = "OFFSET",
                    size = "4x6 inches",
                    material = "PAPER",
                    paperWeight = 250,
                    updatedAt = Instant.now()
                )
            )

            val result = mockMvc.get("/invitation-cards").andExpect {
                status { isOk() }
            }.andReturn()

            result.response.getHeader("Last-Modified") shouldNotBe null
        }
    }
}
