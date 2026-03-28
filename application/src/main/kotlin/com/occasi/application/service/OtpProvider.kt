package com.occasi.application.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

interface OtpProvider {
    fun sendOtp(phone: String, otp: String): Boolean
}

@Component
class LoggingOtpProvider : OtpProvider {

    private val logger = LoggerFactory.getLogger(LoggingOtpProvider::class.java)

    override fun sendOtp(phone: String, otp: String): Boolean {
        logger.info("OTP for phone {}: {}", phone, otp)
        return true
    }
}
