package com.occasi.application.controller

import com.occasi.application.config.TestFirebaseConfig
import com.occasi.application.service.GoogleAuthService
import com.occasi.application.service.GoogleUserInfo
import com.occasi.application.service.JwtService
import io.kotest.core.spec.style.StringSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

// Feature: artist-dashboard-panel, Property 18
@SpringBootTest
@Import(TestFirebaseConfig::class)
@AutoConfigureMockMvc
@Transactional
class BookingControllerPropertyTest : StringSpec() {

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
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var jwtService: JwtService

    /** Generator for large non-existent artist IDs */
    private val nonExistentArtistIdArb: Arb<Long> = Arb.long(900000L..999999L)

    init {

        // Property 18: Non-existent artist returns 404 for bookings
        // **Validates: Requirements 7.7**
        "Property 18: GET /bookings/artist/{artistId} returns 404 for non-existent artist" {
            checkAll(
                PropTestConfig(minSuccess = 100),
                nonExistentArtistIdArb
            ) { artistId ->
                mockMvc.perform(
                    get("/bookings/artist/$artistId")
                        .with(user("1").roles("CUSTOMER"))
                )
                    .andExpect(status().isNotFound)
                    .andExpect(jsonPath("$.error").value("Artist not found"))
            }
        }
    }
}
