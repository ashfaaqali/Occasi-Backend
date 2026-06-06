package com.occasi.application.service

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.kotest.matchers.string.shouldEndWith
import io.kotest.property.Arb
import io.kotest.property.arbitrary.byte
import io.kotest.property.arbitrary.byteArray
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.of
import io.kotest.property.checkAll
import java.nio.file.Files
import java.nio.file.Path
import java.util.Base64

// Feature: artist-onboarding-dashboard, Property 3: Image storage round-trip
class ImageStorageServicePropertyTest : StringSpec({

    val uploadDir = Path.of(System.getProperty("user.home"), "data", "uploads")
    val service = ImageStorageService()

    beforeSpec {
        service.init()
    }

    afterSpec {
        // Clean up test files
        if (Files.exists(uploadDir)) {
            Files.walk(uploadDir)
                .sorted(Comparator.reverseOrder())
                .forEach { 
                    // Only delete files, keep directory structure to avoid deleting other users' data
                    if (it != uploadDir) {
                        Files.deleteIfExists(it) 
                    }
                }
        }
    }

    // Validates: Requirements 2.6, 2.8, 2.9
    "storing base64-encoded bytes and reading the file at the returned path yields the exact same bytes" {
        checkAll(100,
            Arb.byteArray(Arb.int(1..1024), Arb.byte()),
            Arb.of("photo.jpg", "image.png", "art.webp", null)
        ) { originalBytes, fileName ->
            val base64Data = Base64.getEncoder().encodeToString(originalBytes)

            val returnedPath = service.store(base64Data, fileName)

            returnedPath shouldStartWith "/uploads/"

            val expectedExtension = fileName?.substringAfterLast('.', "jpg") ?: "jpg"
            returnedPath shouldEndWith ".$expectedExtension"

            val filePath = uploadDir.resolve(returnedPath.substringAfterLast('/'))
            val storedBytes = Files.readAllBytes(filePath)
            storedBytes shouldBe originalBytes

            // Clean up individual file after assertion
            Files.deleteIfExists(filePath)
        }
    }
})
