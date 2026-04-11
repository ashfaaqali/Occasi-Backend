package com.occasi.application.controller

import com.occasi.application.dto.ArtistRegistrationRequest
import com.occasi.application.service.HennaArtistService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/henna-artists")
class HennaArtistController(private val service: HennaArtistService) {

    @GetMapping
    fun getAllHennaArtists(@RequestParam complexity: String?): ResponseEntity<Any> {
        return if (complexity != null) {
            try {
                ResponseEntity.ok(service.getArtistsForComplexity(complexity))
            } catch (e: IllegalArgumentException) {
                ResponseEntity.badRequest().body(mapOf("error" to "Invalid complexity tier: $complexity"))
            }
        } else {
            ResponseEntity.ok(service.getAllHennaArtists())
        }
    }

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
    fun registerArtist(@RequestBody request: ArtistRegistrationRequest): ResponseEntity<Any> {
        if (request.name.isBlank() || request.email.isBlank() || request.mobileNumber.isBlank()) {
            return ResponseEntity.badRequest().body(mapOf("error" to "Name, email, and mobile number are required"))
        }
        val artist = service.registerArtist(request)
        return ResponseEntity.status(201).body(artist)
    }
}