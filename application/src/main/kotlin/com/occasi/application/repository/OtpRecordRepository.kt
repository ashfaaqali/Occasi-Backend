package com.occasi.application.repository

import com.occasi.application.model.OtpRecord
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface OtpRecordRepository : JpaRepository<OtpRecord, Long> {
    fun findByPhoneAndOtp(phone: String, otp: String): OtpRecord?
    fun deleteByPhone(phone: String)
    fun deleteByExpiresAtBefore(time: LocalDateTime)
}
