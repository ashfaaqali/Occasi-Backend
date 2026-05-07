package com.occasi.application.service

import com.occasi.application.model.InvitationCard
import com.occasi.application.repository.InvitationCardRepository
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class InvitationCardService(private val repository: InvitationCardRepository) {

    @Cacheable("invitationCards")
    fun getAllCards(): List<InvitationCard> = repository.findAll()

    @Cacheable("cardDetail", key = "#id")
    fun getCardById(id: Long): InvitationCard? = repository.findById(id).orElse(null)

    fun getCardsByMaterial(material: String): List<InvitationCard> =
        repository.findByMaterial(material.uppercase())

    fun getCardsByFinish(finish: String): List<InvitationCard> =
        repository.findByFinish(finish.uppercase())

    @CacheEvict(value = ["invitationCards", "cardDetail"], allEntries = true)
    fun incrementOrderCount(cardId: Long) {
        val card = repository.findById(cardId).orElseThrow {
            IllegalArgumentException("Invitation card not found")
        }
        card.numberOfOrders += 1
        repository.save(card)
    }

    @CacheEvict(value = ["invitationCards", "cardDetail"], allEntries = true)
    fun recalculateRating(cardId: Long, newRating: Int) {
        val card = repository.findById(cardId).orElseThrow {
            IllegalArgumentException("Invitation card not found")
        }
        card.averageRating = (card.averageRating * card.reviewCount + newRating) / (card.reviewCount + 1)
        card.reviewCount += 1
        repository.save(card)
    }

    @CacheEvict(value = ["invitationCards", "cardDetail"], allEntries = true)
    fun saveCard(card: InvitationCard): InvitationCard {
        require(card.price > 0) { "Price must be a positive integer" }
        require(card.minOrderQuantity > 0) { "minOrderQuantity must be a positive integer" }
        return repository.save(card)
    }

    @CacheEvict(value = ["invitationCards", "cardDetail"], allEntries = true)
    fun deleteCard(id: Long) = repository.deleteById(id)
}
