package com.occasi.application.controller

import com.occasi.application.service.ImageStorageService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.io.IOException

@RestController
@RequestMapping("/images")
class ImageUploadController(private val imageStorageService: ImageStorageService) {

    @PostMapping("/upload")
    fun uploadImage(@RequestBody request: ImageUploadRequest): ResponseEntity<Any> {
        return try {
            val imageUrl = imageStorageService.store(request.base64Data, request.fileName)
            ResponseEntity.ok(ImageUploadResponse(imageUrl = imageUrl))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to "Invalid image data"))
        } catch (e: IOException) {
            ResponseEntity.internalServerError().body(mapOf("error" to "Failed to store image"))
        }
    }
}

data class ImageUploadRequest(val base64Data: String, val fileName: String? = null)
data class ImageUploadResponse(val imageUrl: String)
