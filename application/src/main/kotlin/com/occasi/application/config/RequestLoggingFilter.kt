package com.occasi.application.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
@ConditionalOnProperty(name = ["logging.http.enabled"], havingValue = "true")
class RequestLoggingFilter(
    @Value("\${logging.http.include-headers:false}") private val includeHeaders: Boolean
) : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(RequestLoggingFilter::class.java)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val start = System.currentTimeMillis()

        if (includeHeaders) {
            val headers = buildString {
                val headerNames = request.headerNames
                while (headerNames.hasMoreElements()) {
                    val name = headerNames.nextElement()
                    val value = if (name.equals("Authorization", ignoreCase = true)) {
                        "[REDACTED]"
                    } else {
                        request.getHeader(name)
                    }
                    append("$name: $value")
                    if (headerNames.hasMoreElements()) {
                        append(", ")
                    }
                }
            }
            log.info("Request headers: [{}]", headers)
        }

        filterChain.doFilter(request, response)

        val duration = System.currentTimeMillis() - start
        val queryPart = request.queryString?.let { "?$it" } ?: ""
        log.info("{} {}{} -> {} ({}ms)",
            request.method, request.requestURI, queryPart, response.status, duration)
    }
}
