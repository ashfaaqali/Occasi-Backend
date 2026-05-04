package com.occasi.application.controller

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveAtLeastSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import jakarta.servlet.http.HttpServletRequest
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.slf4j.LoggerFactory
import org.springframework.core.MethodParameter
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.mock.http.MockHttpInputMessage
import org.springframework.validation.BindingResult
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.multipart.MaxUploadSizeExceededException

/**
 * Unit tests for GlobalExceptionHandler edge cases.
 * Validates: Requirements 1.5, 1.6, 1.7, 2.2, 2.3, 2.4
 */
class GlobalExceptionHandlerTest : StringSpec({

    val handler = GlobalExceptionHandler()

    val mockRequest: HttpServletRequest = mock<HttpServletRequest>().also {
        whenever(it.method).thenReturn("POST")
        whenever(it.requestURI).thenReturn("/api/bookings")
    }

    /**
     * Attaches a Logback ListAppender to the GlobalExceptionHandler logger
     * so we can capture and inspect log events in tests.
     */
    fun captureLogEvents(): ListAppender<ILoggingEvent> {
        val logbackLogger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java) as Logger
        val appender = ListAppender<ILoggingEvent>()
        appender.start()
        logbackLogger.addAppender(appender)
        return appender
    }

    // --- Requirement 1.5: MethodArgumentNotValidException → 400 / VALIDATION_ERROR ---

    "MethodArgumentNotValidException returns 400 with VALIDATION_ERROR and field names in message" {
        val bindingResult = mock<BindingResult>()
        val fieldErrors = listOf(
            FieldError("bookingRequest", "date", "must not be null"),
            FieldError("bookingRequest", "artistId", "must be positive")
        )
        whenever(bindingResult.fieldErrors).thenReturn(fieldErrors)

        val methodParameter = mock<MethodParameter>()
        val ex = MethodArgumentNotValidException(methodParameter, bindingResult)

        val response = handler.handleValidation(ex, mockRequest)

        response.statusCode shouldBe HttpStatus.BAD_REQUEST
        response.body!!.code shouldBe "VALIDATION_ERROR"
        response.body!!.error shouldContain "date"
        response.body!!.error shouldContain "artistId"
        response.body!!.error shouldContain "must not be null"
        response.body!!.error shouldContain "must be positive"
    }

    // --- Requirement 1.6: HttpMessageNotReadableException → 400 / MALFORMED_REQUEST ---

    "HttpMessageNotReadableException returns 400 with MALFORMED_REQUEST" {
        val inputMessage = MockHttpInputMessage(ByteArray(0))
        val ex = HttpMessageNotReadableException("Could not read JSON", inputMessage)

        val response = handler.handleMalformedRequest(ex, mockRequest)

        response.statusCode shouldBe HttpStatus.BAD_REQUEST
        response.body!!.code shouldBe "MALFORMED_REQUEST"
        response.body!!.error shouldBe "Request body could not be parsed"
    }

    // --- Requirement 1.7: MaxUploadSizeExceededException → 400 / FILE_TOO_LARGE ---

    "MaxUploadSizeExceededException returns 400 with FILE_TOO_LARGE" {
        val ex = MaxUploadSizeExceededException(5 * 1024 * 1024)

        val response = handler.handleMaxUploadSizeExceeded(ex, mockRequest)

        response.statusCode shouldBe HttpStatus.BAD_REQUEST
        response.body!!.code shouldBe "FILE_TOO_LARGE"
        response.body!!.error shouldContain "5 MB"
    }

    // --- Requirement 2.3: 500-level exceptions logged at ERROR with stack trace ---

    "500-level exception (catch-all) is logged at ERROR level with stack trace" {
        val appender = captureLogEvents()

        val ex = RuntimeException("Something broke badly")
        handler.handleUnexpected(ex, mockRequest)

        val errorEvents = appender.list.filter { it.level == Level.ERROR }
        errorEvents shouldHaveAtLeastSize 1

        val event = errorEvents.last()
        event.message shouldContain "INTERNAL_ERROR"
        // Stack trace is attached as throwable proxy (Requirement 2.2)
        event.throwableProxy.shouldNotBeNull()
    }

    // --- Requirement 2.4: 4xx exceptions logged at WARN without stack trace ---

    "4xx exception (HttpMessageNotReadableException) is logged at WARN level without stack trace" {
        val appender = captureLogEvents()

        val inputMessage = MockHttpInputMessage(ByteArray(0))
        val ex = HttpMessageNotReadableException("bad json", inputMessage)
        handler.handleMalformedRequest(ex, mockRequest)

        val warnEvents = appender.list.filter { it.level == Level.WARN }
        warnEvents shouldHaveAtLeastSize 1

        val event = warnEvents.last()
        event.message shouldContain "MALFORMED_REQUEST"
        // 4xx handlers should NOT attach the exception as a throwable (no stack trace)
        event.throwableProxy.shouldBeNull()
    }

    "4xx exception (MaxUploadSizeExceededException) is logged at WARN without stack trace" {
        val appender = captureLogEvents()

        val ex = MaxUploadSizeExceededException(5 * 1024 * 1024)
        handler.handleMaxUploadSizeExceeded(ex, mockRequest)

        val warnEvents = appender.list.filter { it.level == Level.WARN }
        warnEvents shouldHaveAtLeastSize 1

        val event = warnEvents.last()
        event.message shouldContain "FILE_TOO_LARGE"
        event.throwableProxy.shouldBeNull()
    }
})
