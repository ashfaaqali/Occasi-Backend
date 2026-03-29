package com.occasi.application.exception

class BookingNotFoundException(message: String) : RuntimeException(message)
class InvalidStatusTransitionException(message: String) : RuntimeException(message)
class PaymentVerificationException(message: String) : RuntimeException(message)
class RefundFailedException(message: String) : RuntimeException(message)
class InvalidBookingRequestException(message: String) : RuntimeException(message)
