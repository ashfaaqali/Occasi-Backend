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
    @Value("\${otp.expiry-minutes}") private val expiryMinutes: Long,
    @Value("\${otp.length}") private val otpLength: Int
) {

    @Transactional
    fun generateAndSend(phone: String): String {
        otpRecordRepository.deleteByPhone(phone)

        val otp = generateOtp()
        val expiresAt = LocalDateTime.now().plusMinutes(expiryMinutes)

        val otpRecord = OtpRecord(
            phone = phone,
            otp = otp,
            expiresAt = expiresAt
        )
        otpRecordRepository.save(otpRecord)

        val sent = otpProvider.sendOtp(phone, otp)
        if (!sent) {
            throw OtpSendFailedException("Unable to send OTP. Please try again later.")
        }

        return otp
    }

    @Transactional
    fun verify(phone: String, otp: String) {
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
