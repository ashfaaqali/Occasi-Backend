package com.occasi.application.config

import com.occasi.application.service.JwtService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthFilter(
    private val jwtService: JwtService
) : OncePerRequestFilter() {

    private val publicArtistAuthPaths = setOf(
        "/artist-auth/login",
        "/artist-auth/register",
        "/artist-auth/send-email-otp",
        "/artist-auth/verify-email-otp",
        "/artist-auth/send-phone-otp",
        "/artist-auth/verify-phone-otp",
        "/artist-auth/refresh",
        "/artist-auth/forgot-password",
        "/artist-auth/reset-password"
    )

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.servletPath
        return path.startsWith("/auth/") ||
            path in publicArtistAuthPaths ||
            path.startsWith("/h2-console/")
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authHeader = request.getHeader("Authorization")

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            val token = authHeader.substring(7)
            val claims = jwtService.validateToken(token)

            if (claims != null) {
                val tokenType = claims["type"] as? String

                if (tokenType == "artist") {
                    // Artist JWT — extract artistId, grant ROLE_ARTIST
                    val artistId = claims["artistId"]?.let { (it as Number).toLong() }
                    if (artistId != null) {
                        val authorities = listOf(SimpleGrantedAuthority("ROLE_ARTIST"))
                        val authentication = UsernamePasswordAuthenticationToken(
                            artistId, null, authorities
                        )
                        SecurityContextHolder.getContext().authentication = authentication
                    }
                } else {
                    // Customer JWT — extract userId and role
                    val userId = claims["userId"]?.let { (it as Number).toLong() }
                    val role = claims["role"] as? String

                    if (userId != null && role != null) {
                        val authorities = listOf(SimpleGrantedAuthority("ROLE_$role"))
                        val authentication = UsernamePasswordAuthenticationToken(
                            userId, null, authorities
                        )
                        SecurityContextHolder.getContext().authentication = authentication
                    }
                }
            }
        }

        filterChain.doFilter(request, response)
    }
}
