package com.occasi.application.controller

import com.occasi.application.constants.BackendRoutes
import com.occasi.application.dto.AssociatePortfolioRequest
import com.occasi.application.dto.PortfolioResponse
import com.occasi.application.service.ArtistPortfolioService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping(BackendRoutes.ArtistPortfolio.BASE)
class ArtistPortfolioController(
    private val artistPortfolioService: ArtistPortfolioService
) {

    @PostMapping(BackendRoutes.ArtistPortfolio.PORTFOLIO)
    fun associatePortfolio(
        @Valid @RequestBody request: AssociatePortfolioRequest,
        authentication: Authentication
    ): ResponseEntity<PortfolioResponse> {
        val artistId = authentication.principal as Long
        val totalImages = artistPortfolioService.associateImages(artistId, request.imageUrls)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(PortfolioResponse(message = "Portfolio updated", totalImages = totalImages))
    }
}
