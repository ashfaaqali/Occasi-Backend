package com.occasi.application.service

import com.occasi.application.constants.BackendMessages
import com.occasi.application.dto.ArtistAuthResponse
import com.occasi.application.dto.ArtistDto
import com.occasi.application.dto.ArtistRegisterRequest
import com.occasi.application.dto.TokenResponse
import com.occasi.application.exception.DuplicateArtistEmailException
import com.occasi.application.exception.InvalidArtistCredentialsException
import com.occasi.application.exception.InvalidArtistRefreshTokenException
import com.occasi.application.model.ArtistPricing
import com.occasi.application.model.ArtistRefreshToken
import com.occasi.application.model.ComplexityTier
import com.occasi.application.model.HennaArtist
import com.occasi.application.repository.ArtistPricingRepository
import com.occasi.application.repository.ArtistRefreshTokenRepository
import com.occasi.application.repository.HennaArtistRepository
import com.occasi.application.util.InputSanitizer
import org.springframework.cache.annotation.CacheEvict
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class ArtistAuthService(
    private val hennaArtistRepository: HennaArtistRepository,
    private val artistPricingRepository: ArtistPricingRepository,
    private val artistRefreshTokenRepository: ArtistRefreshTokenRepository,
    private val otpService: OtpService,
    private val jwtService: JwtService,
    private val passwordEncoder: BCryptPasswordEncoder
) {

    @Transactional
    fun login(email: String, password: String): ArtistAuthResponse {
        val artist = hennaArtistRepository.findByEmail(email)
            ?: throw InvalidArtistCredentialsException()

        if (artist.passwordHash == null || !passwordEncoder.matches(password, artist.passwordHash)) {
            throw InvalidArtistCredentialsException()
        }

        return generateArtistAuthResponse(artist)
    }

    @Transactional
    @CacheEvict(value = ["hennaArtists", "hennaArtistsByCity", "availableCities", "artistDetail", "hennaDesigns", "designDetail"], allEntries = true)
    fun registerArtist(request: ArtistRegisterRequest): ArtistAuthResponse {
        require(request.password.length >= 8) { BackendMessages.Validation.PASSWORD_MIN_LENGTH }

        if (hennaArtistRepository.findByEmail(request.email) != null) {
            throw DuplicateArtistEmailException()
        }

        // Sanitize free-form text fields
        val sanitizedName = InputSanitizer.sanitize(request.name)

        val artist = HennaArtist(
            name = sanitizedName,
            email = request.email,
            mobileNumber = request.mobileNumber,
            cityName = request.cityName ?: "",
            location = request.location ?: "",
            coverImage = request.coverImage,
            passwordHash = passwordEncoder.encode(request.password)
        )

        val savedArtist = hennaArtistRepository.save(artist)

        request.pricingTiers?.let { tiers ->
            require(tiers.values.all { it > 0 }) { "All pricing tier values must be greater than zero" }
            tiers.keys.forEach { key ->
                require(ComplexityTier.entries.any { it.name == key.uppercase() }) {
                    "Invalid complexity tier: $key"
                }
            }

            val pricingEntities = tiers.map { (complexity, price) ->
                ArtistPricing(
                    artist = savedArtist,
                    complexity = ComplexityTier.valueOf(complexity.uppercase()),
                    price = price
                )
            }
            artistPricingRepository.saveAll(pricingEntities)

            savedArtist.startingPrice = tiers.values.minOrNull() ?: 0
            hennaArtistRepository.save(savedArtist)
        } ?: run {
            savedArtist.startingPrice = 0
            hennaArtistRepository.save(savedArtist)
        }

        return generateArtistAuthResponse(savedArtist)
    }

    fun sendEmailOtp(email: String): String {
        return otpService.generateAndSend(email)
    }

    fun verifyEmailOtp(email: String, otp: String) {
        otpService.verify(email, otp)
    }

    fun sendPhoneOtp(phone: String): String {
        return otpService.generateAndSend(phone)
    }

    fun verifyPhoneOtp(phone: String, otp: String) {
        otpService.verify(phone, otp)
    }

    fun refreshToken(refreshToken: String): TokenResponse {
        val storedToken = artistRefreshTokenRepository.findByToken(refreshToken)
            ?: throw InvalidArtistRefreshTokenException()

        if (storedToken.expiresAt.isBefore(LocalDateTime.now())) {
            artistRefreshTokenRepository.deleteByToken(refreshToken)
            throw InvalidArtistRefreshTokenException()
        }

        val accessToken = jwtService.generateArtistAccessToken(storedToken.artist)
        return TokenResponse(accessToken = accessToken)
    }

    @Transactional
    fun logout(refreshToken: String) {
        artistRefreshTokenRepository.deleteByToken(refreshToken)
    }

    fun forgotPassword(email: String) {
        val artist = hennaArtistRepository.findByEmail(email)
        if (artist != null) {
            otpService.generateAndSend(email)
        }
        // Always return without error (anti-enumeration)
    }

    @Transactional
    fun resetPassword(email: String, otp: String, newPassword: String) {
        require(newPassword.length in 8..128) { "Password must be between 8 and 128 characters" }

        otpService.verify(email, otp)

        val artist = hennaArtistRepository.findByEmail(email)
            ?: throw IllegalArgumentException("Invalid or expired OTP")

        artist.passwordHash = passwordEncoder.encode(newPassword)
        hennaArtistRepository.save(artist)

        // Invalidate all refresh tokens (force re-login)
        artistRefreshTokenRepository.deleteByArtistId(artist.id!!)
    }

    private fun generateArtistAuthResponse(artist: HennaArtist): ArtistAuthResponse {
        val accessToken = jwtService.generateArtistAccessToken(artist)
        val refreshTokenStr = jwtService.generateArtistRefreshToken(artist)

        val refreshToken = ArtistRefreshToken(
            token = refreshTokenStr,
            artist = artist,
            expiresAt = LocalDateTime.now().plusDays(7)
        )
        artistRefreshTokenRepository.save(refreshToken)

        return ArtistAuthResponse(
            accessToken = accessToken,
            refreshToken = refreshTokenStr,
            artist = toArtistDto(artist)
        )
    }

    private fun toArtistDto(artist: HennaArtist): ArtistDto {
        return ArtistDto(
            id = artist.id!!,
            name = artist.name,
            email = artist.email,
            mobileNumber = artist.mobileNumber,
            cityName = artist.cityName
        )
    }
}
