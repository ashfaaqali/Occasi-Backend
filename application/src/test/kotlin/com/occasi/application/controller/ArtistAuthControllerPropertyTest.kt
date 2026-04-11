package com.occasi.application.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.occasi.application.dto.*
import com.occasi.application.service.GoogleAuthService
import com.occasi.application.service.GoogleUserInfo
import com.occasi.application.service.JwtService
import io.kotest.core.spec.style.StringSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.string.shouldNotBeBlank
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.checkAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

// Feature: artist-dashboard-panel, Properties 9, 10
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ArtistAuthControllerPropertyTest : StringSpec() {

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
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var jwtService: JwtService

    /** Generator for random email addresses */
    private val emailArb: Arb<String> = arbitrary {
        val user = (1..8).map { _ -> ('a' + it.random.nextInt(0, 26)) }.joinToString("")
        val domain = (1..5).map { _ -> ('a' + it.random.nextInt(0, 26)) }.joinToString("")
        "$user@$domain.com"
    }

    /** Generator for random names */
    private val nameArb: Arb<String> = arbitrary {
        (1..10).map { _ -> ('a' + it.random.nextInt(0, 26)) }.joinToString("")
    }

    /** Generator for random mobile numbers */
    private val mobileArb: Arb<String> = arbitrary {
        val firstDigit = it.random.nextInt(1, 10)
        val rest = (1..9).map { _ -> it.random.nextInt(0, 10) }.joinToString("")
        "$firstDigit$rest"
    }

    /** Generator for random passwords (8+ chars) */
    private val passwordArb: Arb<String> = arbitrary {
        val length = it.random.nextInt(8, 20)
        (1..length).map { _ ->
            val charPool = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
            charPool[it.random.nextInt(0, charPool.length)]
        }.joinToString("")
    }

    /**
     * Helper: register an artist via POST /artist-auth/register and return the auth response.
     */
    private fun registerArtist(email: String, name: String, mobile: String, password: String): ArtistAuthResponse {
        val requestBody = objectMapper.writeValueAsString(
            ArtistRegisterRequest(
                name = name,
                email = email,
                mobileNumber = mobile,
                password = password
            )
        )

        val result = mockMvc.perform(
            post("/artist-auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isCreated)
            .andReturn()

        return objectMapper.readValue(result.response.contentAsString, ArtistAuthResponse::class.java)
    }

    init {

        // Property 9: Artist token refresh returns new access token
        // Validates: Requirements 3.2
        "Property 9: POST /artist-auth/refresh with valid refresh token returns new access token, invalid token returns 401" {
            checkAll(
                PropTestConfig(minSuccess = 100),
                emailArb, nameArb, mobileArb, passwordArb
            ) { email, name, mobile, password ->
                // Register an artist and get a valid refresh token
                val authResponse = registerArtist(email, name, mobile, password)
                val refreshToken = authResponse.refreshToken

                // (a) Refresh with valid token returns 200 with a new valid access token
                val refreshBody = objectMapper.writeValueAsString(
                    ArtistRefreshTokenRequest(refreshToken = refreshToken)
                )

                val refreshResult = mockMvc.perform(
                    post("/artist-auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody)
                )
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.accessToken").exists())
                    .andReturn()

                val tokenResponse = objectMapper.readTree(refreshResult.response.contentAsString)
                val newAccessToken = tokenResponse["accessToken"].asText()
                newAccessToken.shouldNotBeBlank()

                // The new access token should be a valid artist JWT
                val claims = jwtService.validateToken(newAccessToken)
                claims.shouldNotBeNull()

                // (b) Refresh with a non-existent/invalid token returns 401
                val invalidBody = objectMapper.writeValueAsString(
                    ArtistRefreshTokenRequest(refreshToken = "invalid-token-${System.nanoTime()}")
                )

                mockMvc.perform(
                    post("/artist-auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody)
                )
                    .andExpect(status().isUnauthorized)
            }
        }

        // Property 10: Artist logout invalidates refresh token
        // Validates: Requirements 3.3
        "Property 10: POST /artist-auth/logout invalidates refresh token, subsequent refresh returns 401" {
            checkAll(
                PropTestConfig(minSuccess = 100),
                emailArb, nameArb, mobileArb, passwordArb
            ) { email, name, mobile, password ->
                // Register an artist and get a valid refresh token
                val authResponse = registerArtist(email, name, mobile, password)
                val refreshToken = authResponse.refreshToken

                // Logout with the refresh token
                val logoutBody = objectMapper.writeValueAsString(
                    ArtistLogoutRequest(refreshToken = refreshToken)
                )

                mockMvc.perform(
                    post("/artist-auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(logoutBody)
                )
                    .andExpect(status().isOk)

                // Attempt to refresh with the same token — should return 401
                val refreshBody = objectMapper.writeValueAsString(
                    ArtistRefreshTokenRequest(refreshToken = refreshToken)
                )

                mockMvc.perform(
                    post("/artist-auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody)
                )
                    .andExpect(status().isUnauthorized)
            }
        }
    }
}
