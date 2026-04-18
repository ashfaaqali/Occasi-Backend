package com.occasi.application.service

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.ObjectCannedACL
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.util.UUID

@Service
@ConditionalOnProperty(
    name = ["aws.s3.enabled"],
    havingValue = "true",
    matchIfMissing = false
)
class S3StorageService(
    @Value("\${aws.s3.bucket}") private val bucketName: String,
    @Value("\${aws.s3.region}") private val region: String,
    @Value("\${aws.s3.access-key}") private val accessKey: String,
    @Value("\${aws.s3.secret-key}") private val secretKey: String
) {
    private val logger = LoggerFactory.getLogger(S3StorageService::class.java)
    private lateinit var s3Client: S3Client

    @PostConstruct
    fun init() {
        try {
            s3Client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(
                    StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)
                    )
                )
                .build()
            logger.info("S3 client initialized for bucket: $bucketName in region: $region")
        } catch (e: Exception) {
            logger.error("Failed to initialize S3 client", e)
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
        val objectKey = "images/${UUID.randomUUID()}.$extension"

        val putRequest = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(objectKey)
            .contentType(contentType)
            .build()

        s3Client.putObject(putRequest, RequestBody.fromBytes(bytes))

        return "https://$bucketName.s3.$region.amazonaws.com/$objectKey"
    }
}
