package com.occasi.application.dto

/**
 * Standardized error response returned by [com.occasi.application.controller.GlobalExceptionHandler]
 * for all error conditions.
 *
 * @property error Human-readable error message describing what went wrong.
 * @property code Machine-readable UPPER_SNAKE_CASE error code from the error catalog.
 */
data class ErrorResponse(
    val error: String,
    val code: String
)
