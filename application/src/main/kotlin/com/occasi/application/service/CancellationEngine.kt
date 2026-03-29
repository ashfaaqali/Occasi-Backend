package com.occasi.application.service

import com.occasi.application.model.CancelledBy
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDateTime

@Component
class CancellationEngine {

    fun calculateRefundPercentage(serviceTime: LocalDateTime, cancelledBy: CancelledBy): Int {
        if (cancelledBy == CancelledBy.ARTIST) return 100

        val hoursUntilService = Duration.between(LocalDateTime.now(), serviceTime).toMinutes() / 60.0

        return when {
            hoursUntilService > 4 -> 100
            hoursUntilService >= 1 -> 50
            else -> 0
        }
    }
}
