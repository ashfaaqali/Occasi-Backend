package com.occasi.application.service

import com.occasi.application.dto.ArtistRegisterRequest
import com.occasi.application.exception.DuplicateArtistEmailException
import com.occasi.application.exception.InvalidArtistCredentialsException
import com.occasi.application.repository.ArtistRefreshTokenRepository
import com.occasi.application.repository.HennaArtistRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeEmpty
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.checkAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.transaction.annotation.Transactional

// Feature: artist-dashboard-panel, Properties 1, 7, 8
@SpringBootTest
@Transactional
class ArtistAuthServicePropertyTest : StringSpec() {

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
    lateinit var artistAuthService: ArtistAuthService

    @Autowired
    lateinit var hennaArtistRepository: HennaArtistRepository

    @Autowired
    lateinit var artistRefreshTokenRepository: ArtistRefreshTokenRepository

    @Autowired
    lateinit var passwordEncoder: BCryptPasswordEncoder

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
            val charPool = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#"
            charPool[it.random.nextInt(0, charPool.length)]
        }.joinToString("")
    }

    /** Generator for a wrong password that differs from the original */
    private fun wrongPasswordArb(original: String): Arb<String> = arbitrary {
        var wrong: String
        do {
            val length = it.random.nextInt(8, 20)
            wrong = (1..length).map { _ ->
                val charPool = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
                charPool[it.random.nextInt(0, charPool.length)]
            }.joinToString("")
        } while (wrong == original)
        wrong
    }

    init {

        // Property 1: Artist login credential validation
        // Validates: Requirements 3.1, 3.5, 3.6
        "Property 1: correct credentials return tokens, wrong password or unknown email throw InvalidArtistCredentialsException" {
            checkAll(
                PropTestConfig(minSuccess = 100),
                emailArb, nameArb, mobileArb, passwordArb
            ) { email, name, mobile, password ->
                // Clean up any existing artist with this email
                hennaArtistRepository.findByEmail(email)?.let { hennaArtistRepository.delete(it) }
                hennaArtistRepository.flush()

                // Register an artist via the service
                val registerRequest = ArtistRegisterRequest(
                    name = name,
                    email = email,
                    mobileNumber = mobile,
                    password = password
                )
                artistAuthService.registerArtist(registerRequest)

                // (a) Login with correct credentials returns valid tokens
                val response = artistAuthService.login(email, password)
                response.accessToken.shouldNotBeEmpty()
                response.refreshToken.shouldNotBeEmpty()
                response.artist.email shouldBe email

                // Verify the access token is a valid artist JWT
                val claims = jwtService.validateToken(response.accessToken)
                claims.shouldNotBeNull()
                jwtService.getTokenType(response.accessToken) shouldBe "artist"

                // (b) Login with correct email but wrong password throws
                shouldThrow<InvalidArtistCredentialsException> {
                    artistAuthService.login(email, password + "WRONG")
                }

                // (c) Login with non-existent email throws
                shouldThrow<InvalidArtistCredentialsException> {
                    artistAuthService.login("nonexistent_$email", password)
                }
            }
        }

        // Property 7: Artist registration creates artist with hashed password and returns tokens
        // Validates: Requirements 4.9, 4.10
        "Property 7: registration creates artist with bcrypt-hashed password and returns valid tokens with matching profile" {
            checkAll(
                PropTestConfig(minSuccess = 100),
                emailArb, nameArb, mobileArb, passwordArb
            ) { email, name, mobile, password ->
                // Clean up
                hennaArtistRepository.findByEmail(email)?.let { hennaArtistRepository.delete(it) }
                hennaArtistRepository.flush()

                val request = ArtistRegisterRequest(
                    name = name,
                    email = email,
                    mobileNumber = mobile,
                    password = password
                )

                val response = artistAuthService.registerArtist(request)

                // (a) Response contains valid artist JWT tokens
                response.accessToken.shouldNotBeEmpty()
                response.refreshToken.shouldNotBeEmpty()

                val claims = jwtService.validateToken(response.accessToken)
                claims.shouldNotBeNull()
                jwtService.getTokenType(response.accessToken) shouldBe "artist"

                // (b) Response artist profile matches request fields
                response.artist.name shouldBe name
                response.artist.email shouldBe email
                response.artist.mobileNumber shouldBe mobile

                // (c) Stored artist has bcrypt-hashed password that matches original
                val storedArtist = hennaArtistRepository.findByEmail(email)
                storedArtist.shouldNotBeNull()
                storedArtist.passwordHash.shouldNotBeNull()
                passwordEncoder.matches(password, storedArtist.passwordHash) shouldBe true
            }
        }

        // Property 8: Duplicate artist email returns 409
        // Validates: Requirements 4.13
        "Property 8: registering with an already-taken email throws DuplicateArtistEmailException" {
            checkAll(
                PropTestConfig(minSuccess = 100),
                emailArb, nameArb, mobileArb, passwordArb
            ) { email, name, mobile, password ->
                // Clean up
                hennaArtistRepository.findByEmail(email)?.let { hennaArtistRepository.delete(it) }
                hennaArtistRepository.flush()

                // Register the first artist
                val request = ArtistRegisterRequest(
                    name = name,
                    email = email,
                    mobileNumber = mobile,
                    password = password
                )
                artistAuthService.registerArtist(request)

                // Attempt to register a second artist with the same email
                val duplicateRequest = ArtistRegisterRequest(
                    name = "Other$name",
                    email = email,
                    mobileNumber = "9$mobile".take(10),
                    password = "${password}2"
                )

                shouldThrow<DuplicateArtistEmailException> {
                    artistAuthService.registerArtist(duplicateRequest)
                }
            }
        }
    }
}
