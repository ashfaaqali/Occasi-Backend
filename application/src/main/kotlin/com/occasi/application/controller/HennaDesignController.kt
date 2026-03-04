package com.occasi.application.controller

import com.occasi.application.model.HennaDesign
import com.occasi.application.service.HennaDesignService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/designs")
class HennaDesignController(private val service: HennaDesignService) {

    @GetMapping
    fun getAllDesigns(@RequestParam(required = false) minPrice: Int?, 
                      @RequestParam(required = false) maxPrice: Int?): List<HennaDesign> {
        if (minPrice != null && maxPrice != null) {
            return service.getDesignsByPriceRange(minPrice, maxPrice)
        }
        return service.getAllDesigns()
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
