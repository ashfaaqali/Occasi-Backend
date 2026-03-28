package com.occasi.application.repository

import com.occasi.application.model.RefreshToken
import com.occasi.application.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface RefreshTokenRepository : JpaRepository<RefreshToken, Long> {
    fun findByToken(token: String): RefreshToken?
    fun deleteByToken(token: String)
    fun deleteByUser(user: User)
}
