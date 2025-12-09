package com.occasi.application.repository

import com.occasi.application.model.Booking
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface BookingRepository : JpaRepository<Booking, Long> {
    fun findByUserId(userId: Long): List<Booking>
    fun findByArtistId(artistId: Long): List<Booking>
}
