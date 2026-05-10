package com.occasi.application.controller

import com.occasi.application.dto.FavouriteItemResponse
import com.occasi.application.dto.FavouriteRequest
import com.occasi.application.exception.InvalidItemTypeException
import com.occasi.application.model.ItemType
import com.occasi.application.service.FavouritesService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/favourites")
class FavouritesController(private val favouritesService: FavouritesService) {

    @PostMapping
    fun addFavourite(@Valid @RequestBody request: FavouriteRequest): ResponseEntity<FavouriteItemResponse> {
        val userId = getAuthenticatedUserId()
        val itemType = parseItemType(request.itemType)
        favouritesService.addFavourite(userId, request.itemId, itemType)
        val response = favouritesService.getFavourites(userId, itemType).first { it.itemId == request.itemId }
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @DeleteMapping
    fun removeFavourite(@Valid @RequestBody request: FavouriteRequest): ResponseEntity<Void> {
        val userId = getAuthenticatedUserId()
        val itemType = parseItemType(request.itemType)
        favouritesService.removeFavourite(userId, request.itemId, itemType)
        return ResponseEntity.noContent().build()
    }

    @GetMapping
    fun getFavourites(@RequestParam(required = false) item_type: String?): ResponseEntity<List<FavouriteItemResponse>> {
        val userId = getAuthenticatedUserId()
        val itemType = item_type?.let { parseItemType(it) }
        return ResponseEntity.ok(favouritesService.getFavourites(userId, itemType))
    }

    private fun getAuthenticatedUserId(): Long {
        val auth = SecurityContextHolder.getContext().authentication
        return auth.principal as Long
    }

    private fun parseItemType(value: String): ItemType {
        return try {
            ItemType.valueOf(value.uppercase())
        } catch (e: IllegalArgumentException) {
            throw InvalidItemTypeException(value)
        }
    }
}
