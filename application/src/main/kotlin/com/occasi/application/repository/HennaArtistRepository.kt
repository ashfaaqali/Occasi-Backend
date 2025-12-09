package com.occasi.application.repository

import com.occasi.application.model.HennaArtist
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface HennaArtistRepository : JpaRepository<HennaArtist, Long>
