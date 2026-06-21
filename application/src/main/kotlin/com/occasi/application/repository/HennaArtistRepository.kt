package com.occasi.application.repository

import com.occasi.application.model.HennaArtist
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface HennaArtistRepository : JpaRepository<HennaArtist, Long> {
    fun findByEmail(email: String): HennaArtist?
    fun findByCityNameIgnoreCase(cityName: String): List<HennaArtist>

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT h.cityName FROM HennaArtist h ORDER BY h.cityName")
    fun findDistinctCityNames(): List<String>

    @org.springframework.data.jpa.repository.Query("SELECT MAX(h.updatedAt) FROM HennaArtist h")
    fun findMaxUpdatedAt(): java.time.Instant?
}
