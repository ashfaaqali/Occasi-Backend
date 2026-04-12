package com.occasi.application.controller

import com.occasi.application.service.FirebaseStorageService
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockMultipartFile

// Requirements: 2.6, 2.7, 2.8, 10.1
class ImageUploadControllerTest : StringSpec({

    fun createController(
        mockService: FirebaseStorageService = mock()
    ): ImageUploadController {
        whenever(mockService.upload(any(), any(), anyOrNull()))
            .thenReturn("https://storage.googleapis.com/test-bucket/images/test.jpg")
        return ImageUploadController(mockService)
    }

    // Requirement 2.6: empty file → 400
    "empty file returns 400 with 'No image file provided' error" {
        val controller = createController()
        val emptyFile = MockMultipartFile("file", "empty.jpg", "image/jpeg", ByteArray(0))

        val response = controller.uploadImage(emptyFile)

        response.statusCode shouldBe HttpStatus.BAD_REQUEST
        @Suppress("UNCHECKED_CAST")
        val body = response.body as Map<String, Any>
        body["error"] shouldBe "No image file provided"
    }

    // Requirement 2.6: missing file content (null bytes) → 400
    "file with null content returns 400 with 'No image file provided' error" {
        val controller = createController()
        val nullContentFile = MockMultipartFile("file", "missing.jpg", "image/jpeg", null as ByteArray?)

        val response = controller.uploadImage(nullContentFile)

        response.statusCode shouldBe HttpStatus.BAD_REQUEST
        @Suppress("UNCHECKED_CAST")
        val body = response.body as Map<String, Any>
        body["error"] shouldBe "No image file provided"
    }

    // Requirement 2.8: Firebase failure → 500
    "Firebase upload failure returns 500 with retry error message" {
        val failingService = mock<FirebaseStorageService>()
        whenever(failingService.upload(any(), any(), anyOrNull()))
            .thenThrow(RuntimeException("Firebase connection failed"))
        val controller = ImageUploadController(failingService)

        val validFile = MockMultipartFile("file", "photo.jpg", "image/jpeg", ByteArray(100) { 0xFF.toByte() })

        val response = controller.uploadImage(validFile)

        response.statusCode shouldBe HttpStatus.INTERNAL_SERVER_ERROR
        @Suppress("UNCHECKED_CAST")
        val body = response.body as Map<String, Any>
        body["error"] shouldBe "Failed to upload image. Please try again."
    }

    // Requirement 2.7, 10.1: valid file with valid content type returns 200
    "valid JPEG file returns 200 with imageUrl" {
        val controller = createController()
        val validFile = MockMultipartFile("file", "photo.jpg", "image/jpeg", ByteArray(1024) { (it % 256).toByte() })

        val response = controller.uploadImage(validFile)

        response.statusCode shouldBe HttpStatus.OK
        @Suppress("UNCHECKED_CAST")
        val body = response.body as Map<String, Any>
        (body["imageUrl"] as String).isNotBlank() shouldBe true
    }

    // Requirement 2.7, 10.1: valid PNG file returns 200
    "valid PNG file returns 200 with imageUrl" {
        val controller = createController()
        val validFile = MockMultipartFile("file", "photo.png", "image/png", ByteArray(512) { (it % 256).toByte() })

        val response = controller.uploadImage(validFile)

        response.statusCode shouldBe HttpStatus.OK
        @Suppress("UNCHECKED_CAST")
        val body = response.body as Map<String, Any>
        (body["imageUrl"] as String).isNotBlank() shouldBe true
    }

    // Requirement 2.7, 10.1: valid WebP file returns 200
    "valid WebP file returns 200 with imageUrl" {
        val controller = createController()
        val validFile = MockMultipartFile("file", "photo.webp", "image/webp", ByteArray(256) { (it % 256).toByte() })

        val response = controller.uploadImage(validFile)

        response.statusCode shouldBe HttpStatus.OK
        @Suppress("UNCHECKED_CAST")
        val body = response.body as Map<String, Any>
        (body["imageUrl"] as String).isNotBlank() shouldBe true
    }
})
