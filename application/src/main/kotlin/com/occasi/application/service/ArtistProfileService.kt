package com.occasi.application.service

import com.occasi.application.constants.BackendMessages
import com.occasi.application.repository.HennaArtistRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ArtistProfileService(
    private val hennaArtistRepository: HennaArtistRepository
) {

    @Transactional
    fun updateCoverImage(artistId: Long, coverImageUrl: String): String {
        val artist = hennaArtistRepository.findById(artistId)
            .orElseThrow { IllegalArgumentException(BackendMessages.Artist.NOT_FOUND) }

        artist.coverImage = coverImageUrl
        hennaArtistRepository.save(artist)

        return coverImageUrl
    }
}
