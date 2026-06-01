package com.occasi.application.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter

@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    fun passwordEncoder(): BCryptPasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun securityFilterChain(http: HttpSecurity, jwtAuthFilter: JwtAuthFilter): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .headers { headers ->
                headers.contentTypeOptions { }
                headers.frameOptions { it.deny() }
                headers.httpStrictTransportSecurity { it.includeSubDomains(true).maxAgeInSeconds(31536000) }
                headers.contentSecurityPolicy { it.policyDirectives("default-src 'self'") }
                headers.referrerPolicy { it.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER) }
            }
            .exceptionHandling { exceptions ->
                exceptions.authenticationEntryPoint(HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
            }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/auth/**").permitAll()
                    .requestMatchers("/artist-auth/login").permitAll()
                    .requestMatchers("/artist-auth/register").permitAll()
                    .requestMatchers("/artist-auth/send-email-otp").permitAll()
                    .requestMatchers("/artist-auth/verify-email-otp").permitAll()
                    .requestMatchers("/artist-auth/send-phone-otp").permitAll()
                    .requestMatchers("/artist-auth/verify-phone-otp").permitAll()
                    .requestMatchers("/artist-auth/refresh").permitAll()
                    .requestMatchers("/artist-auth/forgot-password").permitAll()
                    .requestMatchers("/artist-auth/reset-password").permitAll()
                    .requestMatchers("/h2-console/**").permitAll()
                    .requestMatchers("/designs/**").permitAll()
                    .requestMatchers("/invitation-cards/**").permitAll()
                    .requestMatchers("/henna-artists/**").permitAll()
                    .requestMatchers("/tags/**").permitAll()
                    .requestMatchers("/users/**").permitAll()
                    .requestMatchers("/uploads/**").permitAll()
                    .anyRequest().authenticated()
            }
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }
}
