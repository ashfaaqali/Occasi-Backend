package com.occasi.application.controller

import com.occasi.application.config.TestFirebaseConfig
import com.occasi.application.service.GoogleAuthService
import com.occasi.application.service.GoogleUserInfo
import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.core.spec.style.StringSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional

// Feature: saved-favourites, Property 5: Invalid item_type is rejected
// Feature: saved-favourites, Property 6: Delete non-existent favourite is idempotent
@SpringBootTest
@Import(TestFirebaseConfig::class)
@AutoConfigureMockMvc
@Transactional
class FavouritesControllerPropertyTest : StringSpec() {

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
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    /** Valid item types that should be accepted (case-insensitive) */
    private val validItemTypes = setOf("ARTIST", "DESIGN", "CARD", "artist", "design", "card", "Artist", "Design", "Card")

    /**
     * Generator for strings that are NOT valid item types (case-insensitive).
     * Generates random alphanumeric strings and filters out any that match ARTIST, DESIGN, or CARD.
     */
    private val invalidItemTypeArb: Arb<String> = Arb.choice(
        // Random alphanumeric strings that are not valid item types
        Arb.string(1..30, Codepoint.alphanumeric()).filter { str ->
            str.uppercase() !in setOf("ARTIST", "DESIGN", "CARD")
        },
        // Specific edge cases: typos, partial matches, extra characters
        Arb.of(
            "ARTISTS", "DESIGNS", "CARDS",
            "ART", "DES", "CAR",
            "artist_type", "UNKNOWN", "PAINTING",
            "123", "null", "undefined",
            "A", "D", "C", "X", "Z",
            "ARTIS", "DESIG", "CAR D",
            "henna", "invitation", "favourite"
        )
    )

    /** Generator for valid item types */
    private val validItemTypeArb: Arb<String> = Arb.of("ARTIST", "DESIGN", "CARD")

    /** Generator for item IDs */
    private val itemIdArb: Arb<Long> = Arb.long(1L..999999L)

    /**
     * Sets up authentication in SecurityContext with a Long principal (userId).
     * This matches how JwtAuthFilter sets up authentication in production.
     */
    private fun setAuthenticatedUser(userId: Long) {
        val authorities = listOf(SimpleGrantedAuthority("ROLE_CUSTOMER"))
        val authentication = UsernamePasswordAuthenticationToken(userId, null, authorities)
        SecurityContextHolder.getContext().authentication = authentication
    }

    init {

        // Feature: saved-favourites, Property 5: Invalid item_type is rejected
        // **Validates: Requirements 2.6**
        "Property 5: POST /api/favourites with invalid item_type returns HTTP 400" {
            checkAll(
                PropTestConfig(minSuccess = 100),
                invalidItemTypeArb,
                itemIdArb
            ) { invalidType, itemId ->
                setAuthenticatedUser(1L)

                val requestBody = mapOf(
                    "itemId" to itemId,
                    "itemType" to invalidType
                )

                mockMvc.perform(
                    post("/api/favourites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody))
                        .requestAttr("org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY",
                            SecurityContextHolder.getContext())
                        .with { request ->
                            request.userPrincipal = SecurityContextHolder.getContext().authentication
                            request
                        }
                )
                    .andExpect(status().isBadRequest)
                    .andExpect(jsonPath("$.code").value("INVALID_ITEM_TYPE"))
            }
        }

        // Feature: saved-favourites, Property 6: Delete non-existent favourite is idempotent
        // **Validates: Requirements 2.7**
        "Property 6: DELETE /api/favourites for non-existent favourite returns HTTP 204 and favourites list unchanged" {
            checkAll(
                PropTestConfig(minSuccess = 100),
                itemIdArb,
                validItemTypeArb
            ) { itemId, itemType ->
                setAuthenticatedUser(1L)

                // First, get the current favourites list
                val getResult = mockMvc.perform(
                    get("/api/favourites")
                        .requestAttr("org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY",
                            SecurityContextHolder.getContext())
                        .with { request ->
                            request.userPrincipal = SecurityContextHolder.getContext().authentication
                            request
                        }
                )
                    .andExpect(status().isOk)
                    .andReturn()

                val favouritesBefore = getResult.response.contentAsString

                // Attempt to delete a favourite that doesn't exist (random high itemId)
                val requestBody = mapOf(
                    "itemId" to itemId,
                    "itemType" to itemType
                )

                mockMvc.perform(
                    delete("/api/favourites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody))
                        .requestAttr("org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY",
                            SecurityContextHolder.getContext())
                        .with { request ->
                            request.userPrincipal = SecurityContextHolder.getContext().authentication
                            request
                        }
                )
                    .andExpect(status().isNoContent)

                // Verify favourites list is unchanged
                val getResultAfter = mockMvc.perform(
                    get("/api/favourites")
                        .requestAttr("org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY",
                            SecurityContextHolder.getContext())
                        .with { request ->
                            request.userPrincipal = SecurityContextHolder.getContext().authentication
                            request
                        }
                )
                    .andExpect(status().isOk)
                    .andReturn()

                val favouritesAfter = getResultAfter.response.contentAsString

                // The favourites list should be unchanged
                assert(favouritesBefore == favouritesAfter) {
                    "Favourites list changed after deleting non-existent favourite (itemId=$itemId, itemType=$itemType)"
                }
            }
        }
    }
}
