package com.occasi.application.controller

import com.occasi.application.service.S3StorageService
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockMultipartFile

class ImageUploadControllerTest : StringSpec({

    fun createController(mockService: S3StorageService = mock()): ImageUploadController {
        whenever(mockService.upload(any(), any(), anyOrNull()))
            .thenReturn("https://test-bucket.s3.us-east-1.amazonaws.com/images/test.jpg")
        return ImageUploadController(mockService)
    }

    "empty file returns 400" {
        val controller = createController()
        val response = controller.uploadImage(MockMultipartFile("file", "empty.jpg", "image/jpeg", ByteArray(0)))
        response.statusCode shouldBe HttpStatus.BAD_REQUEST
        @Suppress("UNCHECKED_CAST")
        (response.body as Map<String, Any>)["error"] shouldBe "No image file provided"
    }

    "null content file returns 400" {
        val controller = createController()
        val response = controller.uploadImage(MockMultipartFile("file", "missing.jpg", "image/jpeg", null as ByteArray?))
        response.statusCode shouldBe HttpStatus.BAD_REQUEST
        @Suppress("UNCHECKED_CAST")
        (response.body as Map<String, Any>)["error"] shouldBe "No image file provided"
    }

    "S3 upload failure returns 500" {
        val failingService = mock<S3StorageService>()
        whenever(failingService.upload(any(), any(), anyOrNull()))
            .thenThrow(RuntimeException("S3 connection failed"))
        val controller = ImageUploadController(failingService)
        val response = controller.uploadImage(MockMultipartFile("file", "photo.jpg", "image/jpeg", ByteArray(100) { 0xFF.toByte() }))
        response.statusCode shouldBe HttpStatus.INTERNAL_SERVER_ERROR
        @Suppress("UNCHECKED_CAST")
        (response.body as Map<String, Any>)["error"] shouldBe "Failed to upload image. Please try again."
    }

    "valid JPEG returns 200 with imageUrl" {
        val controller = createController()
        val response = controller.uploadImage(MockMultipartFile("file", "photo.jpg", "image/jpeg", ByteArray(1024) { (it % 256).toByte() }))
        response.statusCode shouldBe HttpStatus.OK
        @Suppress("UNCHECKED_CAST")
        ((response.body as Map<String, Any>)["imageUrl"] as String).isNotBlank() shouldBe true
    }

    "valid PNG returns 200 with imageUrl" {
        val controller = createController()
        val response = controller.uploadImage(MockMultipartFile("file", "photo.png", "image/png", ByteArray(512) { (it % 256).toByte() }))
        response.statusCode shouldBe HttpStatus.OK
    }

    "valid WebP returns 200 with imageUrl" {
        val controller = createController()
        val response = controller.uploadImage(MockMultipartFile("file", "photo.webp", "image/webp", ByteArray(256) { (it % 256).toByte() }))
        response.statusCode shouldBe HttpStatus.OK
    }
})
