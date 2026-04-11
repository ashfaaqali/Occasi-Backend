package com.occasi.application.service

import com.occasi.application.model.HennaArtist
import com.occasi.application.model.User
import com.occasi.application.model.UserRole
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.longs.shouldBeBetween
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeEmpty
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import java.util.Base64
import javax.crypto.KeyGenerator

// Feature: authentication-security, Property 8: JWT token generation correctness
class JwtServicePropertyTest : StringSpec({

    // Generate a valid base64-encoded 256-bit HMAC key for tests
    val keyGen = KeyGenerator.getInstance("HmacSHA256")
    keyGen.init(256)
    val testSecret = Base64.getEncoder().encodeToString(keyGen.generateKey().encoded)
    val accessTokenExpiryMs = 900_000L   // 15 minutes
    val refreshTokenExpiryMs = 604_800_000L // 7 days

    val jwtService = JwtService(
        secret = testSecret,
        accessTokenExpiry = accessTokenExpiryMs,
        refreshTokenExpiry = refreshTokenExpiryMs
    )

    // Validates: Requirements 4.1, 4.2, 4.3, 4.6, 9.4
    "access token is verifiable, contains correct userId and role claims, and has ~15 min expiry" {
        checkAll(
            Arb.long(1L..1_000_000L),
            Arb.string(1..50),
            Arb.enum<UserRole>()
        ) { userId, name, role ->
            val user = User(id = userId, name = name, role = role)

            val beforeMs = System.currentTimeMillis()
            val accessToken = jwtService.generateAccessToken(user)
            val afterMs = System.currentTimeMillis()

            accessToken.shouldNotBeEmpty()

            // (a) Token is verifiable using HMAC-SHA256 with the server secret
            val claims = jwtService.validateToken(accessToken)
            claims.shouldNotBeNull()

            // (b) Contains the correct user ID in claims
            jwtService.getUserIdFromToken(accessToken) shouldBe userId

            // (c) Contains the correct user role in claims
            jwtService.getRoleFromToken(accessToken) shouldBe role.name

            // (d) Expiry is 15 minutes from issuance
            // JWT exp claim is stored in seconds, so we allow up to 1 second tolerance
            val expiry = claims.expiration.time
            val expectedMin = beforeMs + accessTokenExpiryMs - 1000
            val expectedMax = afterMs + accessTokenExpiryMs + 1000
            expiry.shouldBeBetween(expectedMin, expectedMax)
        }
    }

    // Validates: Requirements 4.2
    "refresh token has ~7 day expiry" {
        checkAll(
            Arb.long(1L..1_000_000L),
            Arb.string(1..50),
            Arb.enum<UserRole>()
        ) { userId, name, role ->
            val user = User(id = userId, name = name, role = role)

            val beforeMs = System.currentTimeMillis()
            val refreshToken = jwtService.generateRefreshToken(user)
            val afterMs = System.currentTimeMillis()

            refreshToken.shouldNotBeEmpty()

            // Refresh token is verifiable
            val claims = jwtService.validateToken(refreshToken)
            claims.shouldNotBeNull()

            // Expiry is 7 days from issuance (1 second tolerance for JWT second-precision)
            val expiry = claims.expiration.time
            val expectedMin = beforeMs + refreshTokenExpiryMs - 1000
            val expectedMax = afterMs + refreshTokenExpiryMs + 1000
            expiry.shouldBeBetween(expectedMin, expectedMax)
        }
    }

    // Feature: artist-dashboard-panel, Property 2: Artist JWT token claims contain artistId and type
    // Validates: Requirements 3.7
    "artist access token contains artistId and type claims, no userId or role, and is verifiable" {
        checkAll(
            PropTestConfig(minSuccess = 100),
            Arb.long(1L..1_000_000L),
            Arb.string(1..50),
            Arb.string(1..50),
            Arb.string(5..15)
        ) { artistId, name, email, mobile ->
            val artist = HennaArtist(
                id = artistId,
                name = name,
                email = email,
                mobileNumber = mobile,
                cityName = "TestCity",
                location = "TestLocation"
            )

            val accessToken = jwtService.generateArtistAccessToken(artist)
            accessToken.shouldNotBeEmpty()

            // (a) Token is verifiable using the same HMAC-SHA256 signing key
            val claims = jwtService.validateToken(accessToken)
            claims.shouldNotBeNull()

            // (b) Contains artistId claim matching the artist's ID
            claims["artistId"].shouldNotBeNull()
            (claims["artistId"] as Number).toLong() shouldBe artistId

            // (c) Contains type claim equal to "artist"
            claims["type"] shouldBe "artist"

            // (d) Does NOT contain userId or role claims
            claims["userId"].shouldBeNull()
            claims["role"].shouldBeNull()

            // (e) getArtistIdFromToken helper returns the correct ID
            jwtService.getArtistIdFromToken(accessToken) shouldBe artistId

            // (f) getTokenType helper returns "artist"
            jwtService.getTokenType(accessToken) shouldBe "artist"
        }
    }
})
