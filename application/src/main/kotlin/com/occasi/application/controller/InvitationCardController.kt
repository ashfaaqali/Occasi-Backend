package com.occasi.application.controller

import com.occasi.application.model.InvitationCard
import com.occasi.application.service.InvitationCardService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/invitation-cards")
class InvitationCardController(private val service: InvitationCardService) {

    @GetMapping
    fun getAllCards(
        @RequestParam(required = false) material: String?,
        @RequestParam(required = false) finish: String?,
        request: HttpServletRequest
    ): ResponseEntity<Any> {
        if (material != null) {
            return ResponseEntity.ok(service.getCardsByMaterial(material))
        }
        if (finish != null) {
            return ResponseEntity.ok(service.getCardsByFinish(finish))
        }

        val cards = service.getAllCards()
        val lastModified = cards.maxOfOrNull { it.updatedAt }

        // Check If-Modified-Since
        val ifModifiedSince = request.getDateHeader("If-Modified-Since")
        if (lastModified != null && ifModifiedSince > 0 && lastModified.toEpochMilli() <= ifModifiedSince) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build()
        }

        return ResponseEntity.ok()
            .lastModified(lastModified?.toEpochMilli() ?: System.currentTimeMillis())
            .body(cards)
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
