package com.occasi.application.controller

import com.occasi.application.service.S3StorageService
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/images")
@ConditionalOnBean(S3StorageService::class)
class ImageUploadController(private val s3StorageService: S3StorageService) {

    companion object {
        const val MAX_FILE_SIZE = 5 * 1024 * 1024L // 5 MB
        val ALLOWED_CONTENT_TYPES = setOf("image/jpeg", "image/png", "image/webp")
    }

    @PostMapping("/upload")
    fun uploadImage(
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<Any> {
        if (file.isEmpty) {
            return ResponseEntity.badRequest()
                .body(mapOf("error" to "No image file provided"))
        }

        val contentType = file.contentType ?: ""
        if (contentType !in ALLOWED_CONTENT_TYPES) {
            return ResponseEntity.badRequest()
                .body(mapOf("error" to "Unsupported image format. Supported formats: JPEG, PNG, WebP"))
        }

        if (file.size > MAX_FILE_SIZE) {
            return ResponseEntity.badRequest()
                .body(mapOf("error" to "File size exceeds the 5 MB limit"))
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
                .body(mapOf("error" to "Failed to upload image. Please try again."))
        }
    }
}
