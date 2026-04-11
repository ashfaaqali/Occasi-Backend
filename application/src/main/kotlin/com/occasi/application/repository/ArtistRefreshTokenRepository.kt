package com.occasi.application.repository

import com.occasi.application.model.ArtistRefreshToken
import com.occasi.application.model.HennaArtist
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ArtistRefreshTokenRepository : JpaRepository<ArtistRefreshToken, Long> {
    fun findByToken(token: String): ArtistRefreshToken?
    fun deleteByToken(token: String)
    fun deleteByArtist(artist: HennaArtist)
}
