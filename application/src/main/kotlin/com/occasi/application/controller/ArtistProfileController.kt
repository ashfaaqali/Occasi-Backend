package com.occasi.application.controller

import com.occasi.application.constants.BackendRoutes
import com.occasi.application.dto.CoverImageResponse
import com.occasi.application.dto.SubmitKycRequest
import com.occasi.application.dto.UpdateCoverImageRequest
import com.occasi.application.service.ArtistProfileService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping(BackendRoutes.ArtistProfile.BASE)
class ArtistProfileController(
    private val artistProfileService: ArtistProfileService
) {

    @PutMapping(BackendRoutes.ArtistProfile.COVER_IMAGE)
    fun updateCoverImage(
        @Valid @RequestBody request: UpdateCoverImageRequest,
        authentication: Authentication
    ): ResponseEntity<CoverImageResponse> {
        val artistId = authentication.principal as Long
        val coverImageUrl = artistProfileService.updateCoverImage(artistId, request.coverImageUrl)
        return ResponseEntity.ok(
            CoverImageResponse(message = "Cover image updated", coverImageUrl = coverImageUrl)
        )
    }

    @PostMapping("/kyc")
    fun submitKyc(
        @Valid @RequestBody request: SubmitKycRequest,
        authentication: Authentication
    ): ResponseEntity<Map<String, String>> {
        val artistId = authentication.principal as Long
        artistProfileService.submitKycDocuments(artistId, request.idFrontUrl, request.idBackUrl)
        return ResponseEntity.ok(mapOf("message" to "KYC documents submitted for review"))
    }
}
