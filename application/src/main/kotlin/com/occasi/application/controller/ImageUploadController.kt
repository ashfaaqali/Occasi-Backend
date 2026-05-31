package com.occasi.application.controller

import com.occasi.application.constants.BackendMessages
import com.occasi.application.constants.BackendRoutes
import com.occasi.application.repository.ArtistPortfolioImageRepository
import com.occasi.application.service.S3StorageService
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping(BackendRoutes.Images.BASE)
@ConditionalOnBean(S3StorageService::class)
class ImageUploadController(
    private val s3StorageService: S3StorageService,
    private val portfolioImageRepository: ArtistPortfolioImageRepository
) {

    companion object {
        const val MAX_FILE_SIZE = 5 * 1024 * 1024L // 5 MB
        val ALLOWED_CONTENT_TYPES = setOf("image/jpeg", "image/png", "image/webp")
    }

    @PostMapping(BackendRoutes.Images.UPLOAD)
    fun uploadImage(
        @RequestParam("file") file: MultipartFile,
        authentication: Authentication
    ): ResponseEntity<Any> {
        // Portfolio limit pre-check
        val artistId = authentication.principal as Long
        val currentCount = portfolioImageRepository.countByArtistId(artistId)
        if (currentCount >= 6) {
            return ResponseEntity.badRequest()
                .body(mapOf("error" to BackendMessages.Upload.PORTFOLIO_LIMIT))
        }
        if (file.isEmpty) {
            return ResponseEntity.badRequest()
                .body(mapOf("error" to BackendMessages.Upload.NO_FILE))
        }

        val contentType = file.contentType ?: ""
        if (contentType !in ALLOWED_CONTENT_TYPES) {
            return ResponseEntity.badRequest()
                .body(mapOf("error" to BackendMessages.Upload.UNSUPPORTED_FORMAT))
        }

        if (file.size > MAX_FILE_SIZE) {
            return ResponseEntity.badRequest()
                .body(mapOf("error" to BackendMessages.Upload.FILE_TOO_LARGE))
        }

        return try {
            val imageUrl = s3StorageService.upload(
                bytes = file.bytes,
                contentType = contentType,
                originalFileName = file.originalFilename
            )
            ResponseEntity.ok(mapOf("imageUrl" to imageUrl))
        } catch (e: Exception) {
            ResponseEntity.internalServerError()
                .body(mapOf("error" to BackendMessages.Upload.UPLOAD_FAILED))
        }
    }
}
