package com.occasi.application.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

interface OtpProvider {
    fun sendOtp(phone: String, otp: String): Boolean
}

interface EmailOtpProvider {
    fun sendOtp(email: String, otp: String): Boolean
}

@Component
class LoggingOtpProvider : OtpProvider {

    private val logger = LoggerFactory.getLogger(LoggingOtpProvider::class.java)

    override fun sendOtp(phone: String, otp: String): Boolean {
        logger.info("OTP for phone {}: {}", phone, otp)
        return true
    }
}

@Component
class LoggingEmailOtpProvider : EmailOtpProvider {

    private val logger = LoggerFactory.getLogger(LoggingEmailOtpProvider::class.java)

    override fun sendOtp(email: String, otp: String): Boolean {
        logger.info("OTP for email {}: {}", email, otp)
        return true
    }
}
