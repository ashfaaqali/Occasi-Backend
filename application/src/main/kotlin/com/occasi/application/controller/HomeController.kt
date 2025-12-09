package com.occasi.application.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class HomeController {

    @GetMapping("/")
    fun home(): Map<String, String> {
        return mapOf(
            "message" to "Welcome to Occasi Backend API",
            "status" to "Running",
            "endpoints" to "Try /henna-artists, /designs, or /users"
        )
    }
}
