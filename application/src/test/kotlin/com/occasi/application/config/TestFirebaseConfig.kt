package com.occasi.application.config

import com.occasi.application.service.S3StorageService
import org.mockito.kotlin.mock
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

/**
 * Test configuration that provides a mock S3StorageService.
 * This prevents the @PostConstruct init() from trying to connect to real AWS
 * during Spring context initialization in tests.
 */
@TestConfiguration
class TestFirebaseConfig {
    @Bean
    @Primary
    fun mockS3StorageService(): S3StorageService {
        return mock<S3StorageService>()
    }
}
