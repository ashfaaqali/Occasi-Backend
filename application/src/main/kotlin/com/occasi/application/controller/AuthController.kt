package com.occasi.application.controller

import com.occasi.application.constants.BackendMessages
import com.occasi.application.constants.BackendRoutes
import com.occasi.application.dto.*
import com.occasi.application.service.AuthService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping(BackendRoutes.Auth.BASE)
class AuthController(private val authService: AuthService) {

    @PostMapping(BackendRoutes.Auth.SEND_OTP)
    fun sendOtp(@Valid @RequestBody request: SendOtpRequest): ResponseEntity<MessageResponse> {
        authService.sendOtp(request.phone)
        return ResponseEntity.ok(MessageResponse(message = BackendMessages.Auth.OTP_SENT))
    }

    @PostMapping(BackendRoutes.Auth.VERIFY_OTP)
    fun verifyOtp(@Valid @RequestBody request: VerifyOtpRequest): ResponseEntity<AuthResponse> {
        val authResponse = authService.verifyOtp(request.phone, request.otp)
        return ResponseEntity.ok(authResponse)
    }

    @PostMapping(BackendRoutes.Auth.VERIFY_PHONE)
    fun verifyPhone(@RequestBody request: VerifyOtpRequest): ResponseEntity<MessageResponse> {
        authService.verifyPhoneOnly(request.phone, request.otp)
        return ResponseEntity.ok(MessageResponse(message = BackendMessages.Auth.PHONE_VERIFIED))
    }

    @PostMapping(BackendRoutes.Auth.SEND_EMAIL_OTP)
    fun sendEmailOtp(@RequestBody request: SendEmailOtpRequest): ResponseEntity<MessageResponse> {
        authService.sendEmailOtp(request.email)
        return ResponseEntity.ok(MessageResponse(message = BackendMessages.Auth.OTP_SENT_TO_EMAIL))
    }

    @PostMapping(BackendRoutes.Auth.VERIFY_EMAIL)
    fun verifyEmail(@RequestBody request: VerifyEmailOtpRequest): ResponseEntity<MessageResponse> {
        authService.verifyEmailOnly(request.email, request.otp)
        return ResponseEntity.ok(MessageResponse(message = BackendMessages.Auth.EMAIL_VERIFIED))
    }

    @PostMapping(BackendRoutes.Auth.GOOGLE)
    fun googleSignIn(@RequestBody request: GoogleSignInRequest): ResponseEntity<AuthResponse> {
        val authResponse = authService.googleSignIn(request.idToken)
        return ResponseEntity.ok(authResponse)
    }

    @PostMapping(BackendRoutes.Auth.REFRESH)
    fun refreshToken(@Valid @RequestBody request: RefreshTokenRequest): ResponseEntity<TokenResponse> {
        val tokenResponse = authService.refreshToken(request.refreshToken)
        return ResponseEntity.ok(tokenResponse)
    }

    @PostMapping(BackendRoutes.Auth.LOGOUT)
    fun logout(@Valid @RequestBody request: LogoutRequest): ResponseEntity<MessageResponse> {
        authService.logout(request.refreshToken)
        return ResponseEntity.ok(MessageResponse(message = BackendMessages.Auth.LOGGED_OUT))
    }
}
