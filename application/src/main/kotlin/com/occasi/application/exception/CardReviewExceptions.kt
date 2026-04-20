package com.occasi.application.exception

class ReviewNotEligibleException(message: String) : RuntimeException(message)
class DuplicateReviewException(message: String) : RuntimeException(message)
class InvalidRatingException(message: String) : RuntimeException(message)
