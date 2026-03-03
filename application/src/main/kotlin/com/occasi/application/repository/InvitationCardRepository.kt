package com.occasi.application.repository

import com.occasi.application.model.InvitationCard
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface InvitationCardRepository : JpaRepository<InvitationCard, Long> {
    fun findByPriceRange(priceRange: String): List<InvitationCard>
    fun findByPriceBetween(minPrice: Int, maxPrice: Int): List<InvitationCard>
    fun findByMaterial(material: String): List<InvitationCard>
    fun findByPaperType(paperType: String): List<InvitationCard>
}
