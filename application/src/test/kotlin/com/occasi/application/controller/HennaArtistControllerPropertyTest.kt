package com.occasi.application.controller

import io.kotest.core.spec.style.StringSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.transaction.annotation.Transactional

// Feature: artist-onboarding-dashboard, Property 6: Missing required fields return 400
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class HennaArtistControllerPropertyTest : StringSpec() {

    override fun extensions() = listOf(SpringExtension)

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    private val arbNonBlank: Arb<String> =
        Arb.string(minSize = 1, maxSize = 20, codepoints = Arb.of(('a'..'z').map { Codepoint(it.code) }))
            .filter { it.isNotBlank() }

    private val arbBlank: Arb<String> = Arb.of("", "   ", "  ")

    private val arbMaybeBlank: Arb<String> = Arb.choice(arbNonBlank, arbBlank)

    init {
        // Validates: Requirements 7.3
        "requests with at least one blank required field return 400" {
            checkAll(100, arbMaybeBlank, arbMaybeBlank, arbMaybeBlank) { name, email, mobileNumber ->
                // Skip if all are non-blank (that's a valid request)
                if (name.isBlank() || email.isBlank() || mobileNumber.isBlank()) {
                    val body = mapOf(
                        "name" to name,
                        "email" to email,
                        "mobileNumber" to mobileNumber,
                        "password" to "validpassword123",
                        "portfolioImageUrls" to emptyList<String>()
                    )

                    mockMvc.post("/henna-artists") {
                        contentType = MediaType.APPLICATION_JSON
                        content = objectMapper.writeValueAsString(body)
                    }.andExpect {
                        status { isBadRequest() }
                    }
                }
            }
        }
    }
}
