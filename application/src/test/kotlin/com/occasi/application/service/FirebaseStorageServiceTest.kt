package com.occasi.application.service

import com.google.cloud.storage.Acl
import com.google.cloud.storage.Blob
import com.google.cloud.storage.Bucket
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldStartWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class FirebaseStorageServiceTest : StringSpec({

    fun createServiceWithMockBucket(): Pair<FirebaseStorageService, Bucket> {
        val mockBlob = mock<Blob>()
        val mockBucket = mock<Bucket> {
            on { name }.thenReturn("test-bucket")
            on { create(any<String>(), any<ByteArray>(), any<String>(), any<Bucket.BlobTargetOption>()) }
                .thenReturn(mockBlob)
        }

        // Create the service and inject the mock bucket via reflection
        // since init() requires real Firebase credentials
        val service = FirebaseStorageService(
            bucketName = "test-bucket",
            credentialsPath = "fake-path.json"
        )
        val bucketField = FirebaseStorageService::class.java.getDeclaredField("bucket")
        bucketField.isAccessible = true
        bucketField.set(service, mockBucket)

        return Pair(service, mockBucket)
    }

    "upload returns URL with images/ prefix and correct extension for JPEG" {
        val (service, _) = createServiceWithMockBucket()

        val url = service.upload("hello".toByteArray(), "image/jpeg", "photo.jpg")

        url shouldStartWith "https://storage.googleapis.com/test-bucket/images/"
        url shouldEndWith ".jpg"
    }

    "upload returns URL with correct extension for PNG" {
        val (service, _) = createServiceWithMockBucket()

        val url = service.upload("hello".toByteArray(), "image/png", "photo.png")

        url shouldStartWith "https://storage.googleapis.com/test-bucket/images/"
        url shouldEndWith ".png"
    }

    "upload returns URL with correct extension for WebP" {
        val (service, _) = createServiceWithMockBucket()

        val url = service.upload("hello".toByteArray(), "image/webp", "photo.webp")

        url shouldStartWith "https://storage.googleapis.com/test-bucket/images/"
        url shouldEndWith ".webp"
    }

    "upload defaults to jpg extension for unknown content type" {
        val (service, _) = createServiceWithMockBucket()

        val url = service.upload("hello".toByteArray(), "image/gif", "photo.gif")

        url shouldEndWith ".jpg"
    }

    "two uploads with same filename produce distinct URLs" {
        val (service, _) = createServiceWithMockBucket()

        val url1 = service.upload("data1".toByteArray(), "image/jpeg", "same-name.jpg")
        val url2 = service.upload("data2".toByteArray(), "image/jpeg", "same-name.jpg")

        url1 shouldNotBe url2
    }

    "upload URL contains the bucket name" {
        val (service, _) = createServiceWithMockBucket()

        val url = service.upload("data".toByteArray(), "image/jpeg", "test.jpg")

        url shouldContain "test-bucket"
    }

    "missing credentials causes init failure" {
        val service = FirebaseStorageService(
            bucketName = "test-bucket",
            credentialsPath = "nonexistent-credentials.json"
        )

        val exception = try {
            service.init()
            null
        } catch (e: Exception) {
            e
        }

        exception shouldNotBe null
    }
})
