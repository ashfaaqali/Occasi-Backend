package com.occasi.application.controller

import com.occasi.application.service.HennaArtistService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/cities")
class CityController(private val hennaArtistService: HennaArtistService) {

    /**
     * Returns cities where at least one artist is registered.
     * Used by the customer home screen city picker.
     */
    @GetMapping("/available")
    fun getAvailableCities(): ResponseEntity<List<String>> {
        val cities = hennaArtistService.getAvailableCities()
        return ResponseEntity.ok(cities)
    }
}
