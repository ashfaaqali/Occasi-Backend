package com.occasi.application.controller

import com.occasi.application.constants.BackendRoutes
import com.occasi.application.model.HennaDesign
import com.occasi.application.service.HennaDesignService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping(BackendRoutes.HennaDesigns.BASE)
class HennaDesignController(private val service: HennaDesignService) {

    @GetMapping
    fun getAllDesigns(
        request: HttpServletRequest
    ): ResponseEntity<Any> {
        val designs = service.getAllDesigns()
        val lastModified = designs.maxOfOrNull { it.updatedAt }

        // Check If-Modified-Since
        val ifModifiedSince = request.getDateHeader("If-Modified-Since")
        if (lastModified != null && ifModifiedSince > 0 && lastModified.toEpochMilli() <= ifModifiedSince) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build()
        }

        return ResponseEntity.ok()
            .lastModified(lastModified?.toEpochMilli() ?: System.currentTimeMillis())
            .body(designs)
    }

    @GetMapping(BackendRoutes.HennaDesigns.BY_ID)
    fun getDesignById(@PathVariable id: Long): ResponseEntity<Any> {
        val design = service.getDesignById(id)
        return if (design != null) {
            ResponseEntity.ok(design)
        } else {
            ResponseEntity.status(404).body(mapOf("error" to "Design with id $id not found"))
        }
    }

    @GetMapping(BackendRoutes.HennaDesigns.BY_COMPLEXITY)
    fun getByComplexity(@PathVariable level: String) = service.getDesignsByComplexity(level)
}
