package com.occasi.application.service

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

// Feature: booking-flow-payments, Property 10: Razorpay signature verification correctness
class RazorpayServicePropertyTest : StringSpec({

    // Validates: Requirements 6.4
    "verifySignature returns true only when signature equals HMAC-SHA256(orderId|paymentId, keySecret)" {
        checkAll(
            Arb.string(1..50),
            Arb.string(1..50),
            Arb.string(1..50),
            Arb.string(1..50)
        ) { orderId, paymentId, keySecret, wrongSignature ->
            val service = RazorpayService(
                keyId = "test_key_id",
                keySecret = keySecret
            )

            // Independently compute the expected HMAC-SHA256 signature
            val data = "$orderId|$paymentId"
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(keySecret.toByteArray(), "HmacSHA256"))
            val correctSignature = mac.doFinal(data.toByteArray())
                .joinToString("") { "%02x".format(it) }

            // Correct signature must return true
            service.verifySignature(orderId, paymentId, correctSignature) shouldBe true

            // Any other signature must return false (unless it happens to match)
            if (wrongSignature != correctSignature) {
                service.verifySignature(orderId, paymentId, wrongSignature) shouldBe false
            }
        }
    }
})
