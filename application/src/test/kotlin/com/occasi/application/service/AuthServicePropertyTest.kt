package com.occasi.application.service

import com.occasi.application.config.TestFirebaseConfig
import com.occasi.application.model.User
import com.occasi.application.model.UserRole
import com.occasi.application.repository.OtpRecordRepository
import com.occasi.application.repository.RefreshTokenRepository
import com.occasi.application.repository.UserRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.longs.shouldBeBetween
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch
import io.kotest.matchers.string.shouldNotBeEmpty
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

// Feature: authentication-security, Properties 2, 3, 4, 5, 7
@SpringBootTest
@Import(TestFirebaseConfig::class)
@Transactional
class AuthServicePropertyTest : StringSpec() {

    override fun extensions() = listOf(SpringExtension)

    @TestConfiguration
    class MockGoogleAuthConfig {
        @Bean
        @Primary
        fun mockGoogleAuthService(): GoogleAuthService {
            return object : GoogleAuthService("mock-client-id") {
                override fun verifyIdToken(idToken: String): GoogleUserInfo {
                    // Parse the idToken as "email|name|sub" for test control
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
    lateinit var authService: AuthService

    @Autowired
    lateinit var otpRecordRepository: OtpRecordRepository

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var refreshTokenRepository: RefreshTokenRepository

    /** Generator for valid 10-digit phone numbers */
    private val validPhoneArb: Arb<String> = arbitrary {
        val firstDigit = it.random.nextInt(1, 10) // 1-9 to avoid leading zero
        val rest = (1..9).map { _ -> it.random.nextInt(0, 10) }.joinToString("")
        "$firstDigit$rest"
    }

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

    /** Generator for random Google sub IDs */
    private val googleSubArb: Arb<String> = arbitrary {
        "google-" + (1..12).map { _ -> ('0' + it.random.nextInt(0, 10)) }.joinToString("")
    }

    init {

        // Property 2: OTP generation stores a 6-digit code with 5-minute expiry
        // Validates: Requirements 1.1, 1.4
        "Property 2: sendOtp stores a 6-digit OTP with 5-minute expiry for any valid phone" {
            checkAll(20, validPhoneArb) { phone ->
                // Clean up before each iteration
                otpRecordRepository.deleteByPhone(phone)
                userRepository.findByMobileNumber(phone)?.let { userRepository.delete(it) }

                val beforeSend = LocalDateTime.now()
                authService.sendOtp(phone)
                val afterSend = LocalDateTime.now()

                // Find the OTP record in the database
                val records = otpRecordRepository.findAll().filter { it.phone == phone }
                records.size shouldBe 1

                val record = records.first()
                // OTP should be exactly 6 digits
                record.otp shouldMatch "^\\d{6}$"

                // Expiry should be ~5 minutes from now (±2 second tolerance)
                val expectedExpiryMin = beforeSend.plusMinutes(5).minusSeconds(2)
                val expectedExpiryMax = afterSend.plusMinutes(5).plusSeconds(2)
                val expiryEpoch = record.expiresAt.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                val minEpoch = expectedExpiryMin.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                val maxEpoch = expectedExpiryMax.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                expiryEpoch.shouldBeBetween(minEpoch, maxEpoch)
            }
        }

        // Property 3: Correct OTP verification returns tokens and creates CUSTOMER user
        // Validates: Requirements 2.1, 2.4, 9.1
        "Property 3: verifyOtp with correct OTP returns tokens and creates CUSTOMER user" {
            checkAll(20, validPhoneArb) { phone ->
                // Clean up
                otpRecordRepository.deleteByPhone(phone)
                userRepository.findByMobileNumber(phone)?.let { userRepository.delete(it) }

                // Send OTP
                authService.sendOtp(phone)

                // Retrieve the OTP from DB
                val otpRecord = otpRecordRepository.findAll().first { it.phone == phone }
                val otpCode = otpRecord.otp

                // Verify with correct OTP
                val response = authService.verifyOtp(phone, otpCode)

                // Should return tokens
                response.accessToken.shouldNotBeEmpty()
                response.refreshToken.shouldNotBeEmpty()

                // User should exist with CUSTOMER role
                val user = userRepository.findByMobileNumber(phone)
                user.shouldNotBeNull()
                user.role shouldBe UserRole.CUSTOMER

                // Response user should have CUSTOMER role
                response.user.role shouldBe "CUSTOMER"
                response.user.mobileNumber shouldBe phone
            }
        }

        // Property 4: Incorrect OTP is rejected
        // Validates: Requirements 2.2
        "Property 4: verifyOtp with incorrect OTP throws InvalidOtpException" {
            checkAll(20, validPhoneArb) { phone ->
                // Clean up
                otpRecordRepository.deleteByPhone(phone)
                userRepository.findByMobileNumber(phone)?.let { userRepository.delete(it) }

                // Send OTP
                authService.sendOtp(phone)

                // Retrieve the actual OTP
                val otpRecord = otpRecordRepository.findAll().first { it.phone == phone }
                val correctOtp = otpRecord.otp

                // Generate a different OTP code
                val wrongOtp = if (correctOtp == "000000") "111111" else "000000"

                // Verify with wrong OTP should throw
                shouldThrow<InvalidOtpException> {
                    authService.verifyOtp(phone, wrongOtp)
                }
            }
        }

        // Property 5: Existing user authentication does not create duplicates
        // Validates: Requirements 2.5, 3.6
        "Property 5: authenticating an existing user does not create duplicate users" {
            checkAll(20, validPhoneArb) { phone ->
                // Clean up
                otpRecordRepository.deleteByPhone(phone)
                userRepository.findByMobileNumber(phone)?.let { userRepository.delete(it) }
                refreshTokenRepository.deleteAll()

                // Pre-create a user with this phone
                val existingUser = userRepository.save(
                    User(mobileNumber = phone, role = UserRole.CUSTOMER)
                )
                val userCountBefore = userRepository.count()

                // Send and verify OTP
                authService.sendOtp(phone)
                val otpRecord = otpRecordRepository.findAll().first { it.phone == phone }
                val response = authService.verifyOtp(phone, otpRecord.otp)

                // User count should not have increased
                val userCountAfter = userRepository.count()
                userCountAfter shouldBe userCountBefore

                // Should return tokens for the existing user
                response.user.id shouldBe existingUser.id
                response.accessToken.shouldNotBeEmpty()
                response.refreshToken.shouldNotBeEmpty()
            }
        }

        // Property 7: Google Sign-In with new email creates CUSTOMER user and returns tokens
        // Validates: Requirements 3.3, 3.5
        "Property 7: googleSignIn with new email creates CUSTOMER user and returns tokens" {
            checkAll(20, emailArb, nameArb, googleSubArb) { email, name, sub ->
                // Clean up any existing user with this email
                userRepository.findByEmail(email)?.let { userRepository.delete(it) }
                userRepository.findByGoogleId(sub)?.let { userRepository.delete(it) }

                // Encode test data as idToken: "email|name|sub"
                val idToken = "$email|$name|$sub"

                val response = authService.googleSignIn(idToken)

                // Should return tokens
                response.accessToken.shouldNotBeEmpty()
                response.refreshToken.shouldNotBeEmpty()

                // New user should exist with CUSTOMER role
                val user = userRepository.findByEmail(email)
                user.shouldNotBeNull()
                user.role shouldBe UserRole.CUSTOMER
                user.name shouldBe name
                user.email shouldBe email
                user.googleId shouldBe sub

                // Response user should reflect correct data
                response.user.role shouldBe "CUSTOMER"
                response.user.email shouldBe email
                response.user.name shouldBe name
            }
        }
    }
}
