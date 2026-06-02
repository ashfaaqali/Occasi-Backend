package com.occasi.application.controller

import com.occasi.application.constants.BackendMessages
import com.occasi.application.constants.BackendRoutes
import com.occasi.application.dto.ArtistRegistrationRequest
import com.occasi.application.service.HennaArtistService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping(BackendRoutes.HennaArtists.BASE)
class HennaArtistController(private val service: HennaArtistService) {

    @GetMapping
    fun getAllHennaArtists(
        @RequestParam complexity: String?,
        @RequestParam city: String?,
        request: HttpServletRequest
    ): ResponseEntity<Any> {
        if (complexity != null) {
            return try {
                ResponseEntity.ok(service.getArtistsForComplexity(complexity))
            } catch (e: IllegalArgumentException) {
                ResponseEntity.badRequest().body(mapOf("error" to "Invalid complexity tier: $complexity"))
            }
        }

        val artists = if (city != null) {
            service.getArtistsByCity(city)
        } else {
            service.getAllHennaArtists()
        }

        val lastModified = artists.maxOfOrNull { it.updatedAt }

        // Check If-Modified-Since
        val ifModifiedSince = request.getDateHeader("If-Modified-Since")
        if (lastModified != null && ifModifiedSince > 0 && lastModified.toEpochMilli() <= ifModifiedSince) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build()
        }

        return ResponseEntity.ok()
            .lastModified(lastModified?.toEpochMilli() ?: System.currentTimeMillis())
            .body(artists)
    }

    @GetMapping(BackendRoutes.HennaArtists.BY_ID)
    fun getArtistById(@PathVariable id: Long): ResponseEntity<Any> {
        val artist = service.getArtistById(id)
        return if (artist != null) {
            ResponseEntity.ok(artist)
        } else {
            ResponseEntity.status(404).body(mapOf("error" to "${BackendMessages.Artist.NOT_FOUND}: $id"))
        }
    }

    @PostMapping
    fun registerArtist(@RequestBody request: ArtistRegistrationRequest): ResponseEntity<Any> {
        if (request.name.isBlank() || request.email.isBlank() || request.mobileNumber.isBlank()) {
            return ResponseEntity.badRequest().body(mapOf("error" to BackendMessages.Validation.NAME_EMAIL_PHONE_REQUIRED))
        }
        val artist = service.registerArtist(request)
        return ResponseEntity.status(201).body(artist)
    }
}