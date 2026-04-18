package com.occasi.application.service

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldStartWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectResponse

class S3StorageServiceTest : StringSpec({

    fun createServiceWithMockClient(): S3StorageService {
        val mockS3Client = mock<S3Client>()
        whenever(mockS3Client.putObject(any<PutObjectRequest>(), any<RequestBody>()))
            .thenReturn(PutObjectResponse.builder().build())

        val service = S3StorageService(
            bucketName = "test-bucket",
            region = "us-east-1",
            accessKey = "test-key",
            secretKey = "test-secret"
        )
        // Inject mock S3 client via reflection
        val clientField = S3StorageService::class.java.getDeclaredField("s3Client")
        clientField.isAccessible = true
        clientField.set(service, mockS3Client)

        return service
    }

    "upload returns URL with images/ prefix and correct extension for JPEG" {
        val service = createServiceWithMockClient()
        val url = service.upload("hello".toByteArray(), "image/jpeg", "photo.jpg")

        url shouldStartWith "https://test-bucket.s3.us-east-1.amazonaws.com/images/"
        url shouldEndWith ".jpg"
    }

    "upload returns URL with correct extension for PNG" {
        val service = createServiceWithMockClient()
        val url = service.upload("hello".toByteArray(), "image/png", "photo.png")

        url shouldStartWith "https://test-bucket.s3.us-east-1.amazonaws.com/images/"
        url shouldEndWith ".png"
    }

    "upload returns URL with correct extension for WebP" {
        val service = createServiceWithMockClient()
        val url = service.upload("hello".toByteArray(), "image/webp", "photo.webp")

        url shouldStartWith "https://test-bucket.s3.us-east-1.amazonaws.com/images/"
        url shouldEndWith ".webp"
    }

    "upload defaults to jpg extension for unknown content type" {
        val service = createServiceWithMockClient()
        val url = service.upload("hello".toByteArray(), "image/gif", "photo.gif")

        url shouldEndWith ".jpg"
    }

    "two uploads with same filename produce distinct URLs" {
        val service = createServiceWithMockClient()

        val url1 = service.upload("data1".toByteArray(), "image/jpeg", "same-name.jpg")
        val url2 = service.upload("data2".toByteArray(), "image/jpeg", "same-name.jpg")

        url1 shouldNotBe url2
    }

    "upload URL contains the bucket name" {
        val service = createServiceWithMockClient()
        val url = service.upload("data".toByteArray(), "image/jpeg", "test.jpg")

        url shouldContain "test-bucket"
    }

    "upload URL contains the region" {
        val service = createServiceWithMockClient()
        val url = service.upload("data".toByteArray(), "image/jpeg", "test.jpg")

        url shouldContain "us-east-1"
    }
})
