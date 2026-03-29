package com.occasi.application.service

import com.occasi.application.model.CancelledBy
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import java.time.LocalDateTime

// Feature: booking-flow-payments, Property 1: Cancellation refund percentage follows tiered policy
class CancellationEnginePropertyTest : StringSpec({

    val engine = CancellationEngine()

    // **Validates: Requirements 9.4**
    "artist cancellation always returns 100% regardless of timing" {
        // Generate random minutes offset (from past to far future) — timing should not matter
        checkAll(Arb.long(-120L..600L)) { minutesFromNow ->
            val serviceTime = LocalDateTime.now().plusMinutes(minutesFromNow)
            engine.calculateRefundPercentage(serviceTime, CancelledBy.ARTIST) shouldBe 100
        }
    }

    // **Validates: Requirements 9.1**
    "customer cancellation more than 4 hours before service returns 100%" {
        // Generate minutes well above 4h boundary (242..1440) with buffer for clock drift
        checkAll(Arb.long(242L..1440L)) { minutesFromNow ->
            val serviceTime = LocalDateTime.now().plusMinutes(minutesFromNow)
            engine.calculateRefundPercentage(serviceTime, CancelledBy.CUSTOMER) shouldBe 100
        }
    }

    // **Validates: Requirements 9.2**
    "customer cancellation between 1 and 4 hours before service returns 50%" {
        // Generate minutes safely within the 1-4h window (62..238) with buffer for clock drift
        checkAll(Arb.long(62L..238L)) { minutesFromNow ->
            val serviceTime = LocalDateTime.now().plusMinutes(minutesFromNow)
            engine.calculateRefundPercentage(serviceTime, CancelledBy.CUSTOMER) shouldBe 50
        }
    }

    // **Validates: Requirements 9.3**
    "customer cancellation less than 1 hour before service returns 0%" {
        // Generate minutes safely under 1h (1..58) with buffer for clock drift
        checkAll(Arb.long(1L..58L)) { minutesFromNow ->
            val serviceTime = LocalDateTime.now().plusMinutes(minutesFromNow)
            engine.calculateRefundPercentage(serviceTime, CancelledBy.CUSTOMER) shouldBe 0
        }
    }
})
