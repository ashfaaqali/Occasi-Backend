package com.occasi.application.controller

import com.occasi.application.constants.BackendMessages
import com.occasi.application.constants.BackendRoutes
import com.occasi.application.dto.*
import com.occasi.application.service.ArtistAuthService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping(BackendRoutes.ArtistAuth.BASE)
class ArtistAuthController(private val artistAuthService: ArtistAuthService) {

    @PostMapping(BackendRoutes.ArtistAuth.LOGIN)
    fun login(@Valid @RequestBody request: ArtistLoginRequest): ResponseEntity<ArtistAuthResponse> {
        val response = artistAuthService.login(request.email, request.password)
        return ResponseEntity.ok(response)
    }

    @PostMapping(BackendRoutes.ArtistAuth.REGISTER)
    fun register(@Valid @RequestBody request: ArtistRegisterRequest): ResponseEntity<ArtistAuthResponse> {
        val response = artistAuthService.registerArtist(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @PostMapping(BackendRoutes.ArtistAuth.SEND_EMAIL_OTP)
    fun sendEmailOtp(@Valid @RequestBody request: SendEmailOtpRequest): ResponseEntity<MessageResponse> {
        artistAuthService.sendEmailOtp(request.email)
        return ResponseEntity.ok(MessageResponse(message = BackendMessages.Auth.OTP_SENT))
    }

    @PostMapping(BackendRoutes.ArtistAuth.VERIFY_EMAIL_OTP)
    fun verifyEmailOtp(@RequestBody request: VerifyEmailOtpRequest): ResponseEntity<MessageResponse> {
        artistAuthService.verifyEmailOtp(request.email, request.otp)
        return ResponseEntity.ok(MessageResponse(message = BackendMessages.Auth.EMAIL_VERIFIED))
    }

    @PostMapping(BackendRoutes.ArtistAuth.SEND_PHONE_OTP)
    fun sendPhoneOtp(@RequestBody request: SendPhoneOtpRequest): ResponseEntity<MessageResponse> {
        artistAuthService.sendPhoneOtp(request.phone)
        return ResponseEntity.ok(MessageResponse(message = BackendMessages.Auth.OTP_SENT))
    }

    @PostMapping(BackendRoutes.ArtistAuth.VERIFY_PHONE_OTP)
    fun verifyPhoneOtp(@RequestBody request: VerifyPhoneOtpRequest): ResponseEntity<MessageResponse> {
        artistAuthService.verifyPhoneOtp(request.phone, request.otp)
        return ResponseEntity.ok(MessageResponse(message = BackendMessages.Auth.PHONE_VERIFIED))
    }

    @PostMapping(BackendRoutes.ArtistAuth.REFRESH)
    fun refreshToken(@Valid @RequestBody request: ArtistRefreshTokenRequest): ResponseEntity<TokenResponse> {
        val response = artistAuthService.refreshToken(request.refreshToken)
        return ResponseEntity.ok(response)
    }

    @PostMapping(BackendRoutes.ArtistAuth.LOGOUT)
    fun logout(@Valid @RequestBody request: ArtistLogoutRequest): ResponseEntity<MessageResponse> {
        artistAuthService.logout(request.refreshToken)
        return ResponseEntity.ok(MessageResponse(message = BackendMessages.Auth.LOGGED_OUT))
    }

    @PostMapping(BackendRoutes.ArtistAuth.FORGOT_PASSWORD)
    fun forgotPassword(@Valid @RequestBody request: ForgotPasswordRequest): ResponseEntity<MessageResponse> {
        artistAuthService.forgotPassword(request.email)
        return ResponseEntity.ok(MessageResponse(message = BackendMessages.Auth.FORGOT_PASSWORD_SENT))
    }

    @PostMapping(BackendRoutes.ArtistAuth.RESET_PASSWORD)
    fun resetPassword(@Valid @RequestBody request: ResetPasswordRequest): ResponseEntity<MessageResponse> {
        artistAuthService.resetPassword(request.email, request.otp, request.newPassword)
        return ResponseEntity.ok(MessageResponse(message = BackendMessages.Auth.PASSWORD_RESET))
    }
}
