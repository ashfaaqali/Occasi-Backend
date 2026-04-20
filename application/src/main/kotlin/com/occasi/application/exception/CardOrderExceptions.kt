package com.occasi.application.exception

class CardOrderNotFoundException(message: String) : RuntimeException(message)
class DuplicateSampleOrderException(message: String) : RuntimeException(message)
class InvalidOrderQuantityException(message: String) : RuntimeException(message)
class InvalidOrderStatusTransitionException(message: String) : RuntimeException(message)
