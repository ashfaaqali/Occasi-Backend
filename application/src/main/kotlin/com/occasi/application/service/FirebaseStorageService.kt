package com.occasi.application.service

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.storage.Acl
import com.google.cloud.storage.Bucket
import com.google.cloud.storage.Storage
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.cloud.StorageClient
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
    name = ["firebase.enabled"],
    havingValue = "true",
    matchIfMissing = true
)
class FirebaseStorageService(
    @Value("\${firebase.storage.bucket}") private val bucketName: String,
    @Value("\${firebase.credentials.path}") private val credentialsPath: String
) {
    private val logger = LoggerFactory.getLogger(FirebaseStorageService::class.java)
    private lateinit var bucket: Bucket

    @PostConstruct
    fun init() {
        try {
            val credentialsStream = ClassPathResource(credentialsPath).inputStream
            val options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(credentialsStream))
                .setStorageBucket(bucketName)
                .build()

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options)
            }

            bucket = StorageClient.getInstance().bucket()
        } catch (e: Exception) {
            logger.error("Firebase service account credentials not found at: $credentialsPath", e)
            throw e
        }
    }

    fun upload(bytes: ByteArray, contentType: String, originalFileName: String?): String {
        val extension = when (contentType) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }
        val objectName = "images/${UUID.randomUUID()}.$extension"

        val blob = bucket.create(
            objectName,
            bytes,
            contentType,
            Bucket.BlobTargetOption.doesNotExist()
        )

        blob.createAcl(Acl.of(Acl.User.ofAllUsers(), Acl.Role.READER))

        return "https://storage.googleapis.com/${bucket.name}/$objectName"
    }
}
