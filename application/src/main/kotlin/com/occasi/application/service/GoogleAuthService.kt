package com.occasi.application.service

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

data class GoogleUserInfo(
    val email: String,
    val name: String,
    val sub: String
)

class InvalidGoogleTokenException(message: String) : RuntimeException(message)

@Service
class GoogleAuthService(
    @Value("\${google.client-id}") private val clientId: String
) {

    private val verifier: GoogleIdTokenVerifier by lazy {
        GoogleIdTokenVerifier.Builder(NetHttpTransport(), GsonFactory.getDefaultInstance())
            .setAudience(listOf(clientId))
            .build()
    }

    fun verifyIdToken(idToken: String): GoogleUserInfo {
        val googleIdToken = try {
            verifier.verify(idToken)
        } catch (e: Exception) {
            throw InvalidGoogleTokenException("Invalid Google credentials")
        } ?: throw InvalidGoogleTokenException("Invalid Google credentials")

        val payload = googleIdToken.payload
        val email = payload.email
            ?: throw InvalidGoogleTokenException("Invalid Google credentials")
        val name = (payload["name"] as? String) ?: ""
        val sub = payload.subject
            ?: throw InvalidGoogleTokenException("Invalid Google credentials")

        return GoogleUserInfo(email = email, name = name, sub = sub)
    }
}
