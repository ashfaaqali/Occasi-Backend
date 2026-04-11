package com.occasi.application.service

import com.occasi.application.dto.ArtistRegistrationRequest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotBeBlank
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import io.kotest.extensions.spring.SpringExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional

// Feature: artist-onboarding-dashboard, Property 5: Artist registration round-trip
// Feature: artist-onboarding-dashboard, Property 7: Default values for optional fields
// Feature: artist-onboarding-dashboard, Property 4: Portfolio images excluded from design catalog
@SpringBootTest
@Transactional
class HennaArtistServicePropertyTest : StringSpec() {

    override fun extensions() = listOf(SpringExtension)

    @Autowired
    lateinit var service: HennaArtistService

    @Autowired
    lateinit var designService: HennaDesignService

    private fun arbNonBlankString(): Arb<String> =
        Arb.string(minSize = 1, maxSize = 50, codepoints = Arb.of((('a'..'z') + ('A'..'Z') + ('0'..'9')).map { Codepoint(it.code) }))
            .filter { it.isNotBlank() }

    private fun arbEmail(): Arb<String> =
        arbNonBlankString().map { "$it@example.com" }

    private fun arbPhoneNumber(): Arb<String> =
        Arb.string(minSize = 10, maxSize = 10, codepoints = Arb.of(('0'..'9').map { Codepoint(it.code) }))

    private fun arbImageUrl(): Arb<String> =
        arbNonBlankString().map { "/uploads/$it.jpg" }

    init {
        // Validates: Requirements 7.1, 7.2, 7.5
        "registering an artist and fetching by ID yields the same data" {
            checkAll(100,
                arbNonBlankString(),
                arbEmail(),
                arbPhoneNumber(),
                Arb.list(arbImageUrl(), 0..5)
            ) { name, email, mobileNumber, portfolioUrls ->
                val request = ArtistRegistrationRequest(
                    name = name,
                    email = email,
                    mobileNumber = mobileNumber,
                    password = "testpass1234",
                    cityName = "TestCity",
                    location = "TestLocation",
                    portfolioImageUrls = portfolioUrls
                )

                val created = service.registerArtist(request)
                created.id shouldNotBe null

                val fetched = service.getArtistById(created.id!!)
                fetched shouldNotBe null
                fetched!!.name shouldBe name
                fetched.email shouldBe email
                fetched.mobileNumber shouldBe mobileNumber
                fetched.portfolioImages.map { it.imageUrl } shouldContainExactlyInAnyOrder portfolioUrls
            }
        }

        // Validates: Requirements 7.4
        "omitting cityName and location yields non-null non-blank defaults" {
            checkAll(100,
                arbNonBlankString(),
                arbEmail(),
                arbPhoneNumber()
            ) { name, email, mobileNumber ->
                val request = ArtistRegistrationRequest(
                    name = name,
                    email = email,
                    mobileNumber = mobileNumber,
                    password = "testpass1234",
                    cityName = null,
                    location = null,
                    portfolioImageUrls = emptyList()
                )

                val created = service.registerArtist(request)
                created.cityName.shouldNotBeBlank()
                created.location.shouldNotBeBlank()
            }
        }

        // Validates: Requirements 6.1, 6.3
        "portfolio images are excluded from design catalog" {
            checkAll(100,
                arbNonBlankString(),
                arbEmail(),
                arbPhoneNumber(),
                Arb.list(arbImageUrl(), 1..5)
            ) { name, email, mobileNumber, portfolioUrls ->
                val request = ArtistRegistrationRequest(
                    name = name,
                    email = email,
                    mobileNumber = mobileNumber,
                    password = "testpass1234",
                    portfolioImageUrls = portfolioUrls
                )

                service.registerArtist(request)

                val allDesigns = designService.getAllDesigns()
                val portfolioUrlSet = portfolioUrls.toSet()
                val designImageUrls = allDesigns.map { it.imageUrl }

                designImageUrls.none { it in portfolioUrlSet } shouldBe true
            }
        }
    }
}
