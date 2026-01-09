package com.occasi.application.service

import com.occasi.application.model.InvitationCard
import com.occasi.application.repository.InvitationCardRepository
import org.springframework.stereotype.Service

@Service
class InvitationCardService(private val repository: InvitationCardRepository) {

    fun getAllCards(): List<InvitationCard> = repository.findAll()

    fun getCardById(id: Long): InvitationCard? = repository.findById(id).orElse(null)

    fun getCardsByOccasion(category: String): List<InvitationCard> =
        repository.findByOccasionCategory(category.uppercase())

    fun getCardsByPriceRange(priceRange: String): List<InvitationCard> =
        repository.findByPriceRange(priceRange.uppercase())

    fun getCardsByPriceBetween(minPrice: Int, maxPrice: Int): List<InvitationCard> =
        repository.findByPriceBetween(minPrice, maxPrice)

    fun getCardsByMaterial(material: String): List<InvitationCard> =
        repository.findByMaterial(material.uppercase())

    fun getCardsByPaperType(paperType: String): List<InvitationCard> =
        repository.findByPaperType(paperType.uppercase())

    fun saveCard(card: InvitationCard): InvitationCard = repository.save(card)

    fun deleteCard(id: Long) = repository.deleteById(id)
}
