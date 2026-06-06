package com.occasi.application.config

import com.occasi.application.exception.RateLimitExceededException
import com.occasi.application.service.JwtService
import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

@Component
class RateLimitInterceptor(
    private val jwtService: JwtService,
    private val env: Environment
) : HandlerInterceptor {

    private val buckets = ConcurrentHashMap<String, Bucket>()

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        if (env.activeProfiles.contains("test")) {
            return true
        }
        val ip = request.getHeader("X-Forwarded-For")?.split(",")?.first()?.trim() ?: request.remoteAddr
        val path = request.requestURI
        val group = resolveGroup(path)

        // IP-based check
        val ipBucket = buckets.computeIfAbsent("ip:$ip:$group") { createBucket(group) }
        val ipProbe = ipBucket.tryConsumeAndReturnRemaining(1)
        if (!ipProbe.isConsumed) {
            response.addHeader("X-RateLimit-Remaining", "0")
            response.addHeader("X-RateLimit-Retry-After", ipProbe.nanosToWaitForRefill.div(1_000_000_000).toString())
            throw RateLimitExceededException()
        }
        response.addHeader("X-RateLimit-Remaining", ipProbe.remainingTokens.toString())

        // User-based check (authenticated requests only)
        val token = request.getHeader("Authorization")?.removePrefix("Bearer ")
        if (!token.isNullOrBlank()) {
            runCatching {
                val userId = jwtService.getUserIdFromToken(token)
                val userBucket = buckets.computeIfAbsent("user:$userId:$group") { createBucket(group) }
                val userProbe = userBucket.tryConsumeAndReturnRemaining(1)
                if (!userProbe.isConsumed) {
                    response.addHeader("X-RateLimit-Remaining", "0")
                    response.addHeader("X-RateLimit-Retry-After", userProbe.nanosToWaitForRefill.div(1_000_000_000).toString())
                    throw RateLimitExceededException()
                }
            }
        }

        return true
    }

    private fun resolveGroup(path: String): String = when {
        path.contains("forgot-password") || path.contains("reset-password") ||
        path.contains("send-otp") || path.contains("send-email-otp") -> "otp"
        path.startsWith("/artist-auth") || path.startsWith("/auth") -> "auth"
        else -> "api"
    }

    private fun createBucket(group: String): Bucket {
        val limit = when (group) {
            "otp" -> 5L
            "auth" -> 10L
            else -> 100L
        }
        return Bucket.builder()
            .addLimit(Bandwidth.builder().capacity(limit).refillGreedy(limit, Duration.ofMinutes(1)).build())
            .build()
    }
}
