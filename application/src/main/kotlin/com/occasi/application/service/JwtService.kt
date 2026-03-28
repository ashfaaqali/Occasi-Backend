package com.occasi.application.service

import com.occasi.application.model.User
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.*
import javax.crypto.SecretKey

@Service
class JwtService(
    @Value("\${jwt.secret}") private val secret: String,
    @Value("\${jwt.access-token-expiry}") private val accessTokenExpiry: Long,
    @Value("\${jwt.refresh-token-expiry}") private val refreshTokenExpiry: Long
) {

    private val signingKey: SecretKey by lazy {
        Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret))
    }

    fun generateAccessToken(user: User): String {
        val now = Date()
        return Jwts.builder()
            .subject(user.id.toString())
            .claim("userId", user.id)
            .claim("role", user.role.name)
            .issuedAt(now)
            .expiration(Date(now.time + accessTokenExpiry))
            .signWith(signingKey)
            .compact()
    }

    fun generateRefreshToken(user: User): String {
        val now = Date()
        return Jwts.builder()
            .subject(user.id.toString())
            .issuedAt(now)
            .expiration(Date(now.time + refreshTokenExpiry))
            .signWith(signingKey)
            .compact()
    }

    fun validateToken(token: String): Claims? {
        return try {
            Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .payload
        } catch (e: Exception) {
            null
        }
    }

    fun getUserIdFromToken(token: String): Long? {
        val claims = validateToken(token) ?: return null
        return claims["userId"]?.let { (it as Number).toLong() }
    }

    fun getRoleFromToken(token: String): String? {
        val claims = validateToken(token) ?: return null
        return claims["role"] as? String
    }
}
