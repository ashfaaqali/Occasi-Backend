package com.occasi.application.service

import com.occasi.application.model.HennaArtist
import com.occasi.application.repository.HennaArtistRepository
import org.springframework.stereotype.Service

@Service
class HennaArtistService(private val repository: HennaArtistRepository) {

    fun getAllHennaArtists(): List<HennaArtist> = repository.findAll()

    fun registerArtist(artist: HennaArtist): HennaArtist {
        // Ensure bidirectional relationship is set
        artist.designs.forEach { it.artist = artist }
        return repository.save(artist)
    }
}