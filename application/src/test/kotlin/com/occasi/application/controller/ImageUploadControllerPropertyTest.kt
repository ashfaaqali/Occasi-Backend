package com.occasi.application.controller

import com.occasi.application.service.S3StorageService
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockMultipartFile
import java.util.UUID

// Feature: image-handling
@OptIn(io.kotest.common.ExperimentalKotest::class)
class ImageUploadControllerPropertyTest : StringSpec({

    val validContentTypeArb: Arb<String> = Arb.of("image/jpeg", "image/png", "image/webp")
    val smallValidFileSizeArb: Arb<Int> = Arb.int(1..4096)
    val oversizedFileSizeArb: Arb<Int> = Arb.int((5 * 1024 * 1024 + 1)..(6 * 1024 * 1024))
    val invalidContentTypeArb: Arb<String> = Arb.of(
        "image/gif", "image/bmp", "image/tiff", "image/svg+xml",
        "application/pdf", "text/plain", "video/mp4", "application/octet-stream",
        "image/x-icon", "audio/mpeg"
    )

    fun createController(): ImageUploadController {
        val mockService = mock<S3StorageService>()
        whenever(mockService.upload(any(), any(), anyOrNull())).thenAnswer {
            "https://test-bucket.s3.us-east-1.amazonaws.com/images/${UUID.randomUUID()}.jpg"
        }
        return ImageUploadController(mockService)
    }

    // Property 1: Upload response contains unique URL with correct path prefix
    "Property 1: valid uploads return response with unique URL containing images/ prefix" {
        val controller = createController()
        checkAll(PropTestConfig(minSuccess = 100), validContentTypeArb, smallValidFileSizeArb) { contentType, size ->
            val file = MockMultipartFile("file", "test.jpg", contentType, ByteArray(size) { (it % 256).toByte() })
            val response = controller.uploadImage(file)
            response.statusCode shouldBe HttpStatus.OK
            @Suppress("UNCHECKED_CAST")
            val body = response.body as Map<String, Any>
            (body["imageUrl"] as String) shouldContain "images/"
        }
    }

    "Property 1: two uploads with the same filename produce distinct URLs" {
        val controller = createController()
        checkAll(PropTestConfig(minSuccess = 100), validContentTypeArb, Arb.string(5..20, Codepoint.alphanumeric())) { contentType, filename ->
            val fileBytes = ByteArray(100) { (it % 256).toByte() }
            val r1 = controller.uploadImage(MockMultipartFile("file", "$filename.jpg", contentType, fileBytes))
            val r2 = controller.uploadImage(MockMultipartFile("file", "$filename.jpg", contentType, fileBytes))
            r1.statusCode shouldBe HttpStatus.OK
            r2.statusCode shouldBe HttpStatus.OK
            @Suppress("UNCHECKED_CAST")
            (r1.body as Map<String, Any>)["imageUrl"] shouldNotBe (r2.body as Map<String, Any>)["imageUrl"]
        }
    }

    // Property 2: File size validation rejects oversized uploads
    "Property 2: files larger than 5 MB are rejected with 400" {
        val controller = createController()
        checkAll(PropTestConfig(minSuccess = 100), validContentTypeArb, oversizedFileSizeArb) { contentType, size ->
            val file = MockMultipartFile("file", "big.jpg", contentType, ByteArray(size))
            val response = controller.uploadImage(file)
            response.statusCode shouldBe HttpStatus.BAD_REQUEST
            @Suppress("UNCHECKED_CAST")
            (response.body as Map<String, Any>)["error"] shouldBe "File size exceeds the 5 MB limit"
        }
    }

    "Property 2: files within 5 MB with valid content type are accepted" {
        val controller = createController()
        checkAll(PropTestConfig(minSuccess = 100), validContentTypeArb, smallValidFileSizeArb) { contentType, size ->
            val file = MockMultipartFile("file", "ok.jpg", contentType, ByteArray(size) { (it % 256).toByte() })
            controller.uploadImage(file).statusCode shouldBe HttpStatus.OK
        }
    }

    // Property 3: Content type validation accepts only supported formats
    "Property 3: unsupported content types are rejected with 400" {
        val controller = createController()
        checkAll(PropTestConfig(minSuccess = 100), invalidContentTypeArb, smallValidFileSizeArb) { contentType, size ->
            val file = MockMultipartFile("file", "image.gif", contentType, ByteArray(size) { (it % 256).toByte() })
            val response = controller.uploadImage(file)
            response.statusCode shouldBe HttpStatus.BAD_REQUEST
            @Suppress("UNCHECKED_CAST")
            (response.body as Map<String, Any>)["error"] shouldBe "Unsupported image format. Supported formats: JPEG, PNG, WebP"
        }
    }

    "Property 3: supported content types with valid size are accepted" {
        val controller = createController()
        checkAll(PropTestConfig(minSuccess = 100), validContentTypeArb, smallValidFileSizeArb) { contentType, size ->
            val file = MockMultipartFile("file", "image.jpg", contentType, ByteArray(size) { (it % 256).toByte() })
            controller.uploadImage(file).statusCode shouldBe HttpStatus.OK
        }
    }
})
