package com.occasi.application.exception

class ArtistNotFoundException(message: String = "Artist not found") : RuntimeException(message)
class DuplicateArtistEmailException(message: String = "An artist with this email already exists") : RuntimeException(message)
class InvalidArtistCredentialsException(message: String = "Invalid email or password") : RuntimeException(message)
class InvalidArtistRefreshTokenException(message: String = "Session expired. Please log in again.") : RuntimeException(message)
class ArtistPricingNotFoundException(message: String = "Artist pricing not found for the specified complexity") : RuntimeException(message)
