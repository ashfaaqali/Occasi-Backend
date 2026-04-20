package com.occasi.application.service

import com.occasi.application.config.TestFirebaseConfig
import com.occasi.application.model.OtpRecord
import com.occasi.application.repository.OtpRecordRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@SpringBootTest
@Import(TestFirebaseConfig::class)
@Transactional
class OtpServiceTest {

    @Autowired
    lateinit var otpRecordRepository: OtpRecordRepository

    private val successProvider = object : OtpProvider {
        override fun sendOtp(phone: String, otp: String): Boolean = true
    }

    private val dummyEmailProvider = object : EmailOtpProvider {
        override fun sendOtp(email: String, otp: String): Boolean = true
    }

    @BeforeEach
    fun setUp() {
        otpRecordRepository.deleteAll()
    }

    @Test
    fun `generateAndSend creates a 6-digit OTP and stores it`() {
        val service = OtpService(otpRecordRepository, successProvider, dummyEmailProvider, 5L, 6)
        val otp = service.generateAndSend("1234567890")

        assertTrue(otp.matches(Regex("^\\d{6}$")))
        assertEquals(6, otp.length)
        assertNotNull(otpRecordRepository.findByPhoneAndOtp("1234567890", otp))
    }

    @Test
    fun `generateAndSend deletes previous OTPs for the same phone`() {
        val service = OtpService(otpRecordRepository, successProvider, dummyEmailProvider, 5L, 6)

        val firstOtp = service.generateAndSend("1234567890")
        val secondOtp = service.generateAndSend("1234567890")

        assertNull(otpRecordRepository.findByPhoneAndOtp("1234567890", firstOtp))
        assertNotNull(otpRecordRepository.findByPhoneAndOtp("1234567890", secondOtp))
    }

    @Test
    fun `verify succeeds and deletes OTP when code matches and is not expired`() {
        val service = OtpService(otpRecordRepository, successProvider, dummyEmailProvider, 5L, 6)
        val otp = service.generateAndSend("1234567890")

        service.verify("1234567890", otp)

        assertNull(otpRecordRepository.findByPhoneAndOtp("1234567890", otp))
    }

    @Test
    fun `verify throws InvalidOtpException when OTP does not match`() {
        val service = OtpService(otpRecordRepository, successProvider, dummyEmailProvider, 5L, 6)
        service.generateAndSend("1234567890")

        assertThrows(InvalidOtpException::class.java) {
            service.verify("1234567890", "000000")
        }
    }

    @Test
    fun `verify throws OtpExpiredException when OTP is expired`() {
        otpRecordRepository.save(OtpRecord(
            phone = "9876543210",
            otp = "123456",
            expiresAt = LocalDateTime.now().minusMinutes(1)
        ))
        val service = OtpService(otpRecordRepository, successProvider, dummyEmailProvider, 5L, 6)

        assertThrows(OtpExpiredException::class.java) {
            service.verify("9876543210", "123456")
        }
    }

    @Test
    fun `generateAndSend throws OtpSendFailedException when provider fails`() {
        val failingProvider = object : OtpProvider {
            override fun sendOtp(phone: String, otp: String): Boolean = false
        }
        val service = OtpService(otpRecordRepository, failingProvider, dummyEmailProvider, 5L, 6)

        assertThrows(OtpSendFailedException::class.java) {
            service.generateAndSend("1234567890")
        }
    }

    @Test
    fun `generateAndSend sends OTP via provider with correct phone and code`() {
        var sentPhone: String? = null
        var sentOtp: String? = null
        val capturingProvider = object : OtpProvider {
            override fun sendOtp(phone: String, otp: String): Boolean {
                sentPhone = phone
                sentOtp = otp
                return true
            }
        }
        val service = OtpService(otpRecordRepository, capturingProvider, dummyEmailProvider, 5L, 6)
        val otp = service.generateAndSend("5551234567")

        assertEquals("5551234567", sentPhone)
        assertEquals(otp, sentOtp)
    }
}
