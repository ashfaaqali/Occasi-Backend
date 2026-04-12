package com.occasi.application.controller

import com.occasi.application.service.FirebaseStorageService
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

    /** Arb for valid content types accepted by the controller */
    val validContentTypeArb: Arb<String> = Arb.of("image/jpeg", "image/png", "image/webp")

    /** Arb for small valid file sizes to keep tests fast (1 byte to 4 KB) */
    val smallValidFileSizeArb: Arb<Int> = Arb.int(1..4096)

    /** Arb for oversized file sizes (just over 5 MB to ~6 MB) */
    val oversizedFileSizeArb: Arb<Int> = Arb.int((5 * 1024 * 1024 + 1)..(6 * 1024 * 1024))

    /** Arb for invalid content types */
    val invalidContentTypeArb: Arb<String> = Arb.of(
        "image/gif", "image/bmp", "image/tiff", "image/svg+xml",
        "application/pdf", "text/plain", "video/mp4", "application/octet-stream",
        "image/x-icon", "audio/mpeg"
    )

    /**
     * Creates a controller with a mocked FirebaseStorageService that returns
     * a unique URL containing the images/ prefix for each upload call.
     */
    fun createController(): ImageUploadController {
        val mockService = mock<FirebaseStorageService>()
        whenever(mockService.upload(any(), any(), anyOrNull())).thenAnswer {
            "https://storage.googleapis.com/test-bucket/images/${UUID.randomUUID()}.jpg"
        }
        return ImageUploadController(mockService)
    }

    // Property 1: Upload response contains unique URL with correct path prefix
    // **Validates: Requirements 1.4, 2.2, 2.3**
    "Property 1: valid uploads return response with unique URL containing images/ prefix" {
        val controller = createController()

        checkAll(
            PropTestConfig(minSuccess = 100),
            validContentTypeArb,
            smallValidFileSizeArb
        ) { contentType, size ->
            val fileBytes = ByteArray(size) { (it % 256).toByte() }
            val file = MockMultipartFile("file", "test-image.jpg", contentType, fileBytes)

            val response = controller.uploadImage(file)

            response.statusCode shouldBe HttpStatus.OK

            @Suppress("UNCHECKED_CAST")
            val body = response.body as Map<String, Any>
            val imageUrl = body["imageUrl"] as String
            imageUrl shouldContain "images/"
        }
    }

    // Property 1 (uniqueness part): two uploads produce distinct URLs
    // **Validates: Requirements 1.4, 2.2, 2.3**
    "Property 1: two uploads with the same filename produce distinct URLs" {
        val controller = createController()

        checkAll(
            PropTestConfig(minSuccess = 100),
            validContentTypeArb,
            Arb.string(5..20, Codepoint.alphanumeric())
        ) { contentType, filename ->
            val fileBytes = ByteArray(100) { (it % 256).toByte() }
            val file1 = MockMultipartFile("file", "$filename.jpg", contentType, fileBytes)
            val file2 = MockMultipartFile("file", "$filename.jpg", contentType, fileBytes)

            val response1 = controller.uploadImage(file1)
            val response2 = controller.uploadImage(file2)

            response1.statusCode shouldBe HttpStatus.OK
            response2.statusCode shouldBe HttpStatus.OK

            @Suppress("UNCHECKED_CAST")
            val url1 = (response1.body as Map<String, Any>)["imageUrl"] as String
            @Suppress("UNCHECKED_CAST")
            val url2 = (response2.body as Map<String, Any>)["imageUrl"] as String

            url1 shouldNotBe url2
        }
    }

    // Property 2: File size validation rejects oversized uploads
    // **Validates: Requirements 2.4, 9.1**
    "Property 2: files larger than 5 MB are rejected with 400 and correct error message" {
        val controller = createController()

        checkAll(
            PropTestConfig(minSuccess = 100),
            validContentTypeArb,
            oversizedFileSizeArb
        ) { contentType, size ->
            val fileBytes = ByteArray(size)
            val file = MockMultipartFile("file", "big-image.jpg", contentType, fileBytes)

            val response = controller.uploadImage(file)

            response.statusCode shouldBe HttpStatus.BAD_REQUEST

            @Suppress("UNCHECKED_CAST")
            val body = response.body as Map<String, Any>
            body["error"] shouldBe "File size exceeds the 5 MB limit"
        }
    }

    // Property 2 (acceptance part): files within 5 MB are not rejected due to size
    // **Validates: Requirements 2.4, 9.1**
    "Property 2: files within 5 MB with valid content type are accepted" {
        val controller = createController()

        checkAll(
            PropTestConfig(minSuccess = 100),
            validContentTypeArb,
            smallValidFileSizeArb
        ) { contentType, size ->
            val fileBytes = ByteArray(size) { (it % 256).toByte() }
            val file = MockMultipartFile("file", "ok-image.jpg", contentType, fileBytes)

            val response = controller.uploadImage(file)

            response.statusCode shouldBe HttpStatus.OK
        }
    }

    // Property 3: Content type validation accepts only supported formats
    // **Validates: Requirements 2.5, 9.2**
    "Property 3: unsupported content types are rejected with 400 and correct error message" {
        val controller = createController()

        checkAll(
            PropTestConfig(minSuccess = 100),
            invalidContentTypeArb,
            smallValidFileSizeArb
        ) { contentType, size ->
            val fileBytes = ByteArray(size) { (it % 256).toByte() }
            val file = MockMultipartFile("file", "image.gif", contentType, fileBytes)

            val response = controller.uploadImage(file)

            response.statusCode shouldBe HttpStatus.BAD_REQUEST

            @Suppress("UNCHECKED_CAST")
            val body = response.body as Map<String, Any>
            body["error"] shouldBe "Unsupported image format. Supported formats: JPEG, PNG, WebP"
        }
    }

    // Property 3 (acceptance part): supported content types are accepted
    // **Validates: Requirements 2.5, 9.2**
    "Property 3: supported content types with valid size are accepted" {
        val controller = createController()

        checkAll(
            PropTestConfig(minSuccess = 100),
            validContentTypeArb,
            smallValidFileSizeArb
        ) { contentType, size ->
            val fileBytes = ByteArray(size) { (it % 256).toByte() }
            val file = MockMultipartFile("file", "image.jpg", contentType, fileBytes)

            val response = controller.uploadImage(file)

            response.statusCode shouldBe HttpStatus.OK
        }
    }
})
