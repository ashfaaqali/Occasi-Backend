package com.occasi.application.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "otp_records")
data class OtpRecord(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    var phone: String,
    var otp: String,
    var expiresAt: LocalDateTime,
    var createdAt: LocalDateTime = LocalDateTime.now()
)
