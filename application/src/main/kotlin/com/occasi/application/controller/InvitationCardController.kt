package com.occasi.application.controller

import com.occasi.application.model.InvitationCard
import com.occasi.application.service.InvitationCardService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/invitation-cards")
class InvitationCardController(private val service: InvitationCardService) {

    @GetMapping
    fun getAllCards(
        @RequestParam(required = false) minPrice: Int?,
        @RequestParam(required = false) maxPrice: Int?
    ): List<InvitationCard> {
        if (minPrice != null && maxPrice != null) {
            return service.getCardsByPriceBetween(minPrice, maxPrice)
        }
        return service.getAllCards()
    }

    @GetMapping("/{id}")
    fun getCardById(@PathVariable id: Long): ResponseEntity<InvitationCard> {
        val card = service.getCardById(id)
        return if (card != null) {
            ResponseEntity.ok(card)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/price-range/{range}")
    fun getByPriceRange(@PathVariable range: String): List<InvitationCard> =
        service.getCardsByPriceRange(range)

    @GetMapping("/material/{material}")
    fun getByMaterial(@PathVariable material: String): List<InvitationCard> =
        service.getCardsByMaterial(material)

    @GetMapping("/paper-type/{paperType}")
    fun getByPaperType(@PathVariable paperType: String): List<InvitationCard> =
        service.getCardsByPaperType(paperType)
}
