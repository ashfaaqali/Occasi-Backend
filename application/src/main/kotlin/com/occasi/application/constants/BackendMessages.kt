package com.occasi.application.constants

/**
 * Single source of truth for all backend response and error messages.
 * Organized by domain into nested objects for type-safe, compile-time verified access.
 */
object BackendMessages {

    object Auth {
        const val OTP_SENT = "OTP sent"
        const val OTP_SENT_TO_EMAIL = "OTP sent to email"
        const val PHONE_VERIFIED = "Phone verified"
        const val EMAIL_VERIFIED = "Email verified"
        const val LOGGED_OUT = "Logged out successfully"
        const val FORGOT_PASSWORD_SENT = "If this email is registered, an OTP has been sent"
        const val PASSWORD_RESET = "Password reset successfully"
        const val INVALID_PHONE = "Invalid phone number"
        const val INVALID_OTP = "Invalid OTP"
        const val OTP_EXPIRED = "OTP has expired"
        const val OTP_SEND_FAILED = "Unable to send OTP"
        const val INVALID_GOOGLE_TOKEN = "Invalid Google credentials"
        const val SESSION_EXPIRED = "Session expired"
        const val ARTIST_SESSION_EXPIRED = "Session expired. Please log in again."
    }

    object Artist {
        const val NOT_FOUND = "Artist not found"
        const val DUPLICATE_EMAIL = "An artist with this email already exists"
        const val INVALID_CREDENTIALS = "Invalid email or password"
    }

    object Booking {
        const val NOT_FOUND = "Booking not found"
        const val INVALID_STATUS_TRANSITION = "Invalid status transition"
        const val COMPLETED_CANNOT_CANCEL = "Completed bookings cannot be cancelled"
        const val PAYMENT_VERIFICATION_FAILED = "Payment verification failed"
        const val REFUND_FAILED = "Failed to initiate refund"
        const val INVALID_REQUEST = "Invalid booking request"
        const val CUSTOMER_NAME_REQUIRED = "Customer name is required"
        const val CUSTOMER_PHONE_REQUIRED = "Customer phone is required"
        const val SERVICE_ADDRESS_REQUIRED = "Service address is required"
        const val SCHEDULED_DATETIME_REQUIRED = "Scheduled date/time is required"
        const val PAYMENT_ORDER_FAILED = "Failed to create payment order. Please try again or choose Pay After Service."
    }

    object CardOrder {
        const val NOT_FOUND = "Card order not found"
        const val DUPLICATE_SAMPLE = "Duplicate sample order"
        const val INVALID_QUANTITY = "Invalid order quantity"
        const val INVALID_STATUS_TRANSITION = "Invalid order status transition"
    }

    object CardReview {
        const val NOT_ELIGIBLE = "Not eligible to review this card"
        const val DUPLICATE = "A review already exists for this order"
        const val INVALID_RATING = "Invalid rating value"
    }

    object Upload {
        const val PORTFOLIO_LIMIT = "Maximum portfolio image limit of 6 reached"
        const val NO_FILE = "No image file provided"
        const val UNSUPPORTED_FORMAT = "Unsupported image format. Supported formats: JPEG, PNG, WebP"
        const val FILE_TOO_LARGE = "File size exceeds the 5 MB limit"
        const val UPLOAD_FAILED = "Failed to upload image. Please try again."
    }

    object Validation {
        const val PASSWORD_MIN_LENGTH = "Password must be at least 8 characters"
        const val NAME_EMAIL_PHONE_REQUIRED = "Name, email, and mobile number are required"
        const val INVALID_PRICING = "All pricing values must be positive integers"
        fun validationFailed(fields: String) = "Validation failed: $fields"
    }

    object General {
        const val MALFORMED_REQUEST = "Request body could not be parsed"
        const val RATE_LIMITED = "Too many requests"
        const val UNEXPECTED_ERROR = "An unexpected error occurred"
        const val INVALID_ITEM_TYPE = "Invalid item type"
    }
}
