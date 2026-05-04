package com.occasi.application.config

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveAtLeastSize
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import jakarta.servlet.FilterChain
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

/**
 * Unit tests for RequestLoggingFilter.
 * Validates: Requirements 3.1, 3.2, 3.3, 3.4
 */
class RequestLoggingFilterTest : StringSpec({

    /**
     * Attaches a Logback ListAppender to the RequestLoggingFilter logger
     * so we can capture and inspect log events in tests.
     */
    fun captureLogEvents(): ListAppender<ILoggingEvent> {
        val logbackLogger = LoggerFactory.getLogger(RequestLoggingFilter::class.java) as Logger
        val appender = ListAppender<ILoggingEvent>()
        appender.start()
        logbackLogger.addAppender(appender)
        return appender
    }

    // --- Requirement 3.1, 3.4: Logs method, URI, query params, status, and duration when enabled ---

    "filter logs HTTP method, URI, query params, response status, and duration when enabled" {
        val appender = captureLogEvents()
        val filter = RequestLoggingFilter(includeHeaders = false)

        val request = MockHttpServletRequest("GET", "/api/bookings").apply {
            queryString = "page=1&size=10"
        }
        val response = MockHttpServletResponse().apply {
            status = 200
        }
        val filterChain = mock<FilterChain>()

        filter.doFilter(request, response, filterChain)

        verify(filterChain).doFilter(request, response)

        val logMessages = appender.list.map { it.formattedMessage }
        val requestLog = logMessages.find { it.contains("GET") && it.contains("/api/bookings") }

        requestLog shouldContain "GET"
        requestLog shouldContain "/api/bookings"
        requestLog shouldContain "page=1&size=10"
        requestLog shouldContain "200"
        requestLog shouldContain "ms"
    }

    // --- Requirement 3.2: Redacts Authorization header value ---

    "filter redacts Authorization header value when include-headers is true" {
        val appender = captureLogEvents()
        val filter = RequestLoggingFilter(includeHeaders = true)

        val request = MockHttpServletRequest("POST", "/api/auth/login").apply {
            addHeader("Authorization", "Bearer secret-token-12345")
            addHeader("Content-Type", "application/json")
        }
        val response = MockHttpServletResponse().apply {
            status = 200
        }
        val filterChain = mock<FilterChain>()

        filter.doFilter(request, response, filterChain)

        val headerLog = appender.list.map { it.formattedMessage }
            .find { it.contains("Request headers:") }

        headerLog shouldContain "[REDACTED]"
        headerLog shouldNotContain "secret-token-12345"
        headerLog shouldContain "Content-Type"
        headerLog shouldContain "application/json"
    }

    // --- Requirement 3.1: Headers not logged when include-headers is false ---

    "filter does not log headers when include-headers is false" {
        val appender = captureLogEvents()
        val filter = RequestLoggingFilter(includeHeaders = false)

        val request = MockHttpServletRequest("GET", "/api/artists").apply {
            addHeader("Authorization", "Bearer some-token")
        }
        val response = MockHttpServletResponse().apply {
            status = 200
        }
        val filterChain = mock<FilterChain>()

        filter.doFilter(request, response, filterChain)

        val headerLogs = appender.list.map { it.formattedMessage }
            .filter { it.contains("Request headers:") }
        headerLogs.shouldBeEmpty()

        // Should still log the request summary line
        val summaryLogs = appender.list.map { it.formattedMessage }
            .filter { it.contains("GET") && it.contains("/api/artists") }
        summaryLogs shouldHaveAtLeastSize 1
    }

    // --- Requirement 3.3: Filter not created when logging.http.enabled is false ---

    "RequestLoggingFilter has @ConditionalOnProperty requiring logging.http.enabled=true" {
        val annotation = RequestLoggingFilter::class.java
            .getAnnotation(ConditionalOnProperty::class.java)

        // Verify the annotation is present and correctly configured
        assert(annotation != null) { "RequestLoggingFilter should have @ConditionalOnProperty annotation" }
        assert(annotation!!.name.contains("logging.http.enabled")) {
            "ConditionalOnProperty should check 'logging.http.enabled'"
        }
        assert(annotation.havingValue == "true") {
            "ConditionalOnProperty should require havingValue='true'"
        }
    }
})
