package com.occasi.application.controller

import com.occasi.application.model.HennaArtist
import com.occasi.application.service.HennaArtistService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/henna-artists")
class HennaArtistController(private val service: HennaArtistService) {

    @GetMapping
    fun getAllHennaArtists() = service.getAllHennaArtists()

    @PostMapping
    fun registerArtist(@RequestBody artist: HennaArtist) = service.registerArtist(artist)
}