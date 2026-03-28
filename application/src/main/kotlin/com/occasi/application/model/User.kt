package com.occasi.application.model

import jakarta.persistence.*

enum class UserRole {
    CUSTOMER, ARTIST
}

@Entity
@Table(name = "app_users") // user is a reserved keyword in H2/Postgres
data class User(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    var name: String = "",
    var email: String = "",
    var mobileNumber: String = "",

    @Enumerated(EnumType.STRING)
    var role: UserRole = UserRole.CUSTOMER,

    var googleId: String? = null
)
