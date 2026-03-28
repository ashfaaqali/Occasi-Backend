package com.occasi.application.service

import com.occasi.application.dto.AuthResponse
import com.occasi.application.dto.TokenResponse
import com.occasi.application.dto.UserDto
import com.occasi.application.model.RefreshToken
import com.occasi.application.model.User
import com.occasi.application.model.UserRole
import com.occasi.application.repository.RefreshTokenRepository
import com.occasi.application.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

class InvalidPhoneException(message: String) : RuntimeException(message)
class InvalidRefreshTokenException(message: String) : RuntimeException(message)

@Service
class AuthService(
    private val otpService: OtpService,
    private val googleAuthService: GoogleAuthService,
    private val jwtService: JwtService,
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository
) {

    fun sendOtp(phone: String): String {
        if (!phone.matches(Regex("^\\d{10}$"))) {
            throw InvalidPhoneException("Phone number must be exactly 10 digits")
        }
        return otpService.generateAndSend(phone)
    }

    @Transactional
    fun verifyOtp(phone: String, otp: String): AuthResponse {
        otpService.verify(phone, otp)

        val user = userRepository.findByMobileNumber(phone)
            ?: userRepository.save(
                User(
                    mobileNumber = phone,
                    role = UserRole.CUSTOMER
                )
            )

        return generateAuthResponse(user)
    }

    @Transactional
    fun googleSignIn(idToken: String): AuthResponse {
        val googleUserInfo = googleAuthService.verifyIdToken(idToken)

        val user = userRepository.findByEmail(googleUserInfo.email)
            ?: userRepository.findByGoogleId(googleUserInfo.sub)
            ?: userRepository.save(
                User(
                    name = googleUserInfo.name,
                    email = googleUserInfo.email,
                    googleId = googleUserInfo.sub,
                    role = UserRole.CUSTOMER
                )
            )

        return generateAuthResponse(user)
    }

    fun refreshToken(refreshToken: String): TokenResponse {
        val storedToken = refreshTokenRepository.findByToken(refreshToken)
            ?: throw InvalidRefreshTokenException("Session expired. Please log in again.")

        if (storedToken.expiresAt.isBefore(LocalDateTime.now())) {
            refreshTokenRepository.deleteByToken(refreshToken)
            throw InvalidRefreshTokenException("Session expired. Please log in again.")
        }

        val accessToken = jwtService.generateAccessToken(storedToken.user)
        return TokenResponse(accessToken = accessToken)
    }

    @Transactional
    fun logout(refreshToken: String) {
        refreshTokenRepository.deleteByToken(refreshToken)
    }

    private fun generateAuthResponse(user: User): AuthResponse {
        val accessToken = jwtService.generateAccessToken(user)
        val refreshTokenStr = jwtService.generateRefreshToken(user)

        val refreshToken = RefreshToken(
            token = refreshTokenStr,
            user = user,
            expiresAt = LocalDateTime.now().plusDays(7)
        )
        refreshTokenRepository.save(refreshToken)

        return AuthResponse(
            accessToken = accessToken,
            refreshToken = refreshTokenStr,
            user = toUserDto(user)
        )
    }

    private fun toUserDto(user: User): UserDto {
        return UserDto(
            id = user.id!!,
            name = user.name,
            email = user.email,
            mobileNumber = user.mobileNumber,
            role = user.role.name
        )
    }
}
