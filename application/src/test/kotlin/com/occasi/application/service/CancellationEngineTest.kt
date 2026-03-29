package com.occasi.application.service

import com.occasi.application.model.CancelledBy
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

class CancellationEngineTest : StringSpec({

    val engine = CancellationEngine()

    "artist cancellation returns 100% regardless of timing" {
        val serviceTime = LocalDateTime.now().plusMinutes(10)
        engine.calculateRefundPercentage(serviceTime, CancelledBy.ARTIST) shouldBe 100
    }

    "customer cancellation more than 4 hours before returns 100%" {
        val serviceTime = LocalDateTime.now().plusHours(5)
        engine.calculateRefundPercentage(serviceTime, CancelledBy.CUSTOMER) shouldBe 100
    }

    "customer cancellation between 1 and 4 hours before returns 50%" {
        val serviceTime = LocalDateTime.now().plusHours(2)
        engine.calculateRefundPercentage(serviceTime, CancelledBy.CUSTOMER) shouldBe 50
    }

    "customer cancellation less than 1 hour before returns 0%" {
        val serviceTime = LocalDateTime.now().plusMinutes(30)
        engine.calculateRefundPercentage(serviceTime, CancelledBy.CUSTOMER) shouldBe 0
    }
})
