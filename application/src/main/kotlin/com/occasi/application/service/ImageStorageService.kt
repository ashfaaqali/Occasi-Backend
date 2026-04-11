package com.occasi.application.service

import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path
import java.util.Base64
import java.util.UUID

@Service
class ImageStorageService {
    private val uploadDir = Path.of("./data/uploads")

    @PostConstruct
    fun init() {
        Files.createDirectories(uploadDir)
    }

    fun store(base64Data: String, originalFileName: String?): String {
        val bytes = Base64.getDecoder().decode(base64Data)
        val extension = originalFileName?.substringAfterLast('.', "jpg") ?: "jpg"
        val fileName = "${UUID.randomUUID()}.$extension"
        Files.write(uploadDir.resolve(fileName), bytes)
        return "/uploads/$fileName"
    }
}
