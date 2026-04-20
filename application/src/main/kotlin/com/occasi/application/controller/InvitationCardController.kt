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
        @RequestParam(required = false) material: String?,
        @RequestParam(required = false) finish: String?
    ): List<InvitationCard> {
        if (material != null) {
            return service.getCardsByMaterial(material)
        }
        if (finish != null) {
            return service.getCardsByFinish(finish)
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
}
