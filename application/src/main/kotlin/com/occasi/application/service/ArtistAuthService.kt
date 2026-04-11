package com.occasi.application.service

import com.occasi.application.dto.ArtistAuthResponse
import com.occasi.application.dto.ArtistDto
import com.occasi.application.dto.ArtistRegisterRequest
import com.occasi.application.dto.TokenResponse
import com.occasi.application.exception.DuplicateArtistEmailException
import com.occasi.application.exception.InvalidArtistCredentialsException
import com.occasi.application.exception.InvalidArtistRefreshTokenException
import com.occasi.application.model.ArtistPortfolioImage
import com.occasi.application.model.ArtistPricing
import com.occasi.application.model.ArtistRefreshToken
import com.occasi.application.model.ComplexityTier
import com.occasi.application.model.HennaArtist
import com.occasi.application.repository.ArtistPricingRepository
import com.occasi.application.repository.ArtistRefreshTokenRepository
import com.occasi.application.repository.HennaArtistRepository
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
    fun registerArtist(request: ArtistRegisterRequest): ArtistAuthResponse {
        require(request.password.length >= 8) { "Password must be at least 8 characters" }

        if (hennaArtistRepository.findByEmail(request.email) != null) {
            throw DuplicateArtistEmailException()
        }

        val artist = HennaArtist(
            name = request.name,
            email = request.email,
            mobileNumber = request.mobileNumber,
            cityName = request.cityName ?: "",
            location = request.location ?: "",
            coverImage = request.coverImage,
            passwordHash = passwordEncoder.encode(request.password)
        )

        val portfolioImages = request.portfolioImageUrls.map { url ->
            ArtistPortfolioImage(imageUrl = url).also { it.artist = artist }
        }
        artist.portfolioImages = portfolioImages

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
