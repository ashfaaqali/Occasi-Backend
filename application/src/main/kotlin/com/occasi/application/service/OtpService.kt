package com.occasi.application.service

import com.occasi.application.model.OtpRecord
import com.occasi.application.repository.OtpRecordRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import kotlin.random.Random

@Service
class OtpService(
    private val otpRecordRepository: OtpRecordRepository,
    private val otpProvider: OtpProvider,
    private val emailOtpProvider: EmailOtpProvider,
    @Value("\${otp.expiry-minutes}") private val expiryMinutes: Long,
    @Value("\${otp.length}") private val otpLength: Int,
    @Value("\${otp.test-code:}") private val testCode: String = ""
) {
    @Transactional
    fun generateAndSend(phone: String): String {
        otpRecordRepository.deleteByPhone(phone)
        val otp = generateOtp()
        val expiresAt = LocalDateTime.now().plusMinutes(expiryMinutes)
        otpRecordRepository.save(OtpRecord(phone = phone, otp = otp, expiresAt = expiresAt))
        if (!otpProvider.sendOtp(phone, otp)) {
            throw OtpSendFailedException("Unable to send OTP. Please try again later.")
        }
        return otp
    }

    @Transactional
    fun generateAndSendEmail(email: String): String {
        // Reuse the phone column to store email — it's just a string identifier
        otpRecordRepository.deleteByPhone(email)
        val otp = generateOtp()
        val expiresAt = LocalDateTime.now().plusMinutes(expiryMinutes)
        otpRecordRepository.save(OtpRecord(phone = email, otp = otp, expiresAt = expiresAt))
        if (!emailOtpProvider.sendOtp(email, otp)) {
            throw OtpSendFailedException("Unable to send OTP to email. Please try again later.")
        }
        return otp
    }

    @Transactional
    fun verify(phone: String, otp: String) {
        if (testCode.isNotBlank() && otp == testCode) {
            otpRecordRepository.deleteByPhone(phone)
            return
        }

        val record = otpRecordRepository.findByPhoneAndOtp(phone, otp)
            ?: throw InvalidOtpException("Invalid OTP")

        if (record.expiresAt.isBefore(LocalDateTime.now())) {
            otpRecordRepository.deleteByPhone(phone)
            throw OtpExpiredException("OTP has expired. Please request a new one.")
        }

        otpRecordRepository.deleteByPhone(phone)
    }

    private fun generateOtp(): String {
        val min = Math.pow(10.0, (otpLength - 1).toDouble()).toInt()
        val max = Math.pow(10.0, otpLength.toDouble()).toInt()
        return Random.nextInt(min, max).toString()
    }
}

class InvalidOtpException(message: String) : RuntimeException(message)
class OtpExpiredException(message: String) : RuntimeException(message)
class OtpSendFailedException(message: String) : RuntimeException(message)
