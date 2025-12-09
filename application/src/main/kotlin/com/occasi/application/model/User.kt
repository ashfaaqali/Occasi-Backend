package com.occasi.application.model

import jakarta.persistence.*

@Entity
@Table(name = "app_users") // user is a reserved keyword in H2/Postgres
data class User(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    var name: String,
    var email: String,
    var mobileNumber: String
)
