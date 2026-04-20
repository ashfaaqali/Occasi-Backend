package com.occasi.application.repository

import com.occasi.application.model.InvitationCard
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface InvitationCardRepository : JpaRepository<InvitationCard, Long> {
    fun findByMaterial(material: String): List<InvitationCard>
    fun findByFinish(finish: String): List<InvitationCard>
}
