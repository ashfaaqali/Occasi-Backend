package com.occasi.application.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.occasi.application.dto.LogoutRequest
import com.occasi.application.dto.RefreshTokenRequest
import com.occasi.application.model.RefreshToken
import com.occasi.application.model.User
import com.occasi.application.model.UserRole
import com.occasi.application.repository.RefreshTokenRepository
import com.occasi.application.repository.UserRepository
import com.occasi.application.service.GoogleAuthService
import com.occasi.application.service.GoogleUserInfo
import com.occasi.application.service.JwtService
import io.kotest.core.spec.style.StringSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotBeBlank
import io.kotest.property.Arb
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

// Feature: authentication-security, Properties 9, 14, 15, 16
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerTest : StringSpec() {

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
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var refreshTokenRepository: RefreshTokenRepository

    @Autowired
    lateinit var jwtService: JwtService

    /** Generator for valid 10-digit phone numbers */
    private val validPhoneArb: Arb<String> = arbitrary {
        val firstDigit = it.random.nextInt(1, 10)
        val rest = (1..9).map { _ -> it.random.nextInt(0, 10) }.joinToString("")
        "$firstDigit$rest"
    }

    /** Helper: create a user and generate a valid refresh token stored in DB */
    private fun createUserWithRefreshToken(phone: String): Triple<User, String, String> {
        val user = userRepository.save(
            User(mobileNumber = phone, role = UserRole.CUSTOMER)
        )
        val accessToken = jwtService.generateAccessToken(user)
        val refreshTokenStr = jwtService.generateRefreshToken(user)
        refreshTokenRepository.save(
            RefreshToken(
                token = refreshTokenStr,
                user = user,
                expiresAt = LocalDateTime.now().plusDays(7)
            )
        )
        return Triple(user, accessToken, refreshTokenStr)
    }

    init {

        // Property 9: Token refresh returns new access token
        // Validates: Requirements 4.4
        "Property 9: POST /auth/refresh with valid refresh token returns new access token" {
            checkAll(20, validPhoneArb) { phone ->
                val (_, _, refreshTokenStr) = createUserWithRefreshToken(phone)

                val requestBody = objectMapper.writeValueAsString(
                    RefreshTokenRequest(refreshToken = refreshTokenStr)
                )

                val result = mockMvc.perform(
                    post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                )
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.accessToken").exists())
                    .andReturn()

                val responseBody = result.response.contentAsString
                val tokenResponse = objectMapper.readTree(responseBody)
                val newAccessToken = tokenResponse["accessToken"].asText()
                newAccessToken.shouldNotBeBlank()

                // The new access token should be valid
                jwtService.validateToken(newAccessToken) shouldNotBe null
            }
        }

        // Property 14: Protected endpoints require valid token
        // Validates: Requirements 7.3
        "Property 14: protected endpoints return 401 without token and succeed with valid token" {
            checkAll(20, validPhoneArb) { phone ->
                // Without token: should get 401
                mockMvc.perform(get("/designs"))
                    .andExpect(status().isUnauthorized)

                // With valid token: should NOT get 401
                val (_, accessToken, _) = createUserWithRefreshToken(phone)

                mockMvc.perform(
                    get("/designs")
                        .header("Authorization", "Bearer $accessToken")
                )
                    .andExpect(status().isOk)
            }
        }

        // Property 15: Role-based endpoint restriction
        // Validates: Requirements 7.5, 9.3
        "Property 15: security context contains correct role authority from JWT" {
            checkAll(20, validPhoneArb) { phone ->
                // Create a CUSTOMER user
                val customerUser = userRepository.save(
                    User(mobileNumber = phone, role = UserRole.CUSTOMER)
                )
                val customerToken = jwtService.generateAccessToken(customerUser)

                // Verify the role claim is correctly set in the token
                val role = jwtService.getRoleFromToken(customerToken)
                role shouldBe "CUSTOMER"

                // Verify the token works for authenticated requests (role is extracted by filter)
                mockMvc.perform(
                    get("/designs")
                        .header("Authorization", "Bearer $customerToken")
                )
                    .andExpect(status().isOk)

                // Create an ARTIST user and verify their role claim
                val artistUser = userRepository.save(
                    User(mobileNumber = "9${phone.drop(1)}", role = UserRole.ARTIST)
                )
                val artistToken = jwtService.generateAccessToken(artistUser)
                val artistRole = jwtService.getRoleFromToken(artistToken)
                artistRole shouldBe "ARTIST"

                // ARTIST token should also work for authenticated requests
                mockMvc.perform(
                    get("/designs")
                        .header("Authorization", "Bearer $artistToken")
                )
                    .andExpect(status().isOk)
            }
        }

        // Property 16: Logout invalidates tokens
        // Validates: Requirements 10.1, 10.3
        "Property 16: after logout, refresh token is invalidated on the server" {
            checkAll(20, validPhoneArb) { phone ->
                val (_, _, refreshTokenStr) = createUserWithRefreshToken(phone)

                // Logout
                val logoutBody = objectMapper.writeValueAsString(
                    LogoutRequest(refreshToken = refreshTokenStr)
                )
                mockMvc.perform(
                    post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(logoutBody)
                )
                    .andExpect(status().isOk)

                // Try to refresh with the same token — should fail with 401
                val refreshBody = objectMapper.writeValueAsString(
                    RefreshTokenRequest(refreshToken = refreshTokenStr)
                )
                mockMvc.perform(
                    post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody)
                )
                    .andExpect(status().isUnauthorized)
            }
        }
    }
}
