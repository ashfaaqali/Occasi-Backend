package com.occasi.application.config

import com.occasi.application.service.FirebaseStorageService
import org.mockito.kotlin.mock
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

/**
 * Test configuration that replaces the real FirebaseStorageService with a mock.
 * This prevents the @PostConstruct init() from trying to load real Firebase credentials
 * during Spring context initialization in tests.
 */
@TestConfiguration
class TestFirebaseConfig {
    @Bean
    @Primary
    fun mockFirebaseStorageService(): FirebaseStorageService {
        return mock<FirebaseStorageService>()
    }
}
