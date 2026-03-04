package com.occasi.application.service

import com.occasi.application.model.HennaArtist
import com.occasi.application.repository.HennaArtistRepository
import org.springframework.stereotype.Service

@Service
class HennaArtistService(private val repository: HennaArtistRepository) {

    fun getAllHennaArtists(): List<HennaArtist> = repository.findAll()

    fun getArtistById(id: Long): HennaArtist? = repository.findById(id).orElse(null)

    fun registerArtist(artist: HennaArtist): HennaArtist {
        // Ensure bidirectional relationship is set
        artist.designs.forEach { it.artist = artist }
        artist.startingPrice = artist.designs.minOfOrNull { it.price } ?: 0
        return repository.save(artist)
    }
}