package com.occasi.application.controller

import com.occasi.application.model.HennaArtist
import com.occasi.application.service.HennaArtistService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/henna-artists")
class HennaArtistController(private val service: HennaArtistService) {

    @GetMapping
    fun getAllHennaArtists() = service.getAllHennaArtists()

    @GetMapping("/{id}")
    fun getArtistById(@PathVariable id: Long): ResponseEntity<Any> {
        val artist = service.getArtistById(id)
        return if (artist != null) {
            ResponseEntity.ok(artist)
        } else {
            ResponseEntity.status(404).body(mapOf("error" to "Henna artist with id $id not found"))
        }
    }

    @PostMapping
    fun registerArtist(@RequestBody artist: HennaArtist) = service.registerArtist(artist)
}