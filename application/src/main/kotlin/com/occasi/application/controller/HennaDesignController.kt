package com.occasi.application.controller

import com.occasi.application.model.HennaDesign
import com.occasi.application.service.HennaDesignService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/designs")
class HennaDesignController(private val service: HennaDesignService) {

    @GetMapping
    fun getAllDesigns(
        @RequestParam(required = false) minPrice: Int?,
        @RequestParam(required = false) maxPrice: Int?,
        request: HttpServletRequest
    ): ResponseEntity<Any> {
        if (minPrice != null && maxPrice != null) {
            return ResponseEntity.ok(service.getDesignsByPriceRange(minPrice, maxPrice))
        }

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

    @GetMapping("/{id}")
    fun getDesignById(@PathVariable id: Long): ResponseEntity<Any> {
        val design = service.getDesignById(id)
        return if (design != null) {
            ResponseEntity.ok(design)
        } else {
            ResponseEntity.status(404).body(mapOf("error" to "Design with id $id not found"))
        }
    }

    @GetMapping("/complexity/{level}")
    fun getByComplexity(@PathVariable level: String) = service.getDesignsByComplexity(level)
}
