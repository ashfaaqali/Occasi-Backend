package com.occasi.application.controller

import com.occasi.application.constants.BackendRoutes
import com.occasi.application.model.HennaDesign
import com.occasi.application.model.InvitationCard
import com.occasi.application.service.HennaDesignService
import com.occasi.application.service.InvitationCardService
import com.occasi.application.service.S3StorageService
import com.occasi.application.service.ImageStorageService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.util.Base64

@RestController
@RequestMapping(BackendRoutes.Admin.BASE)
class AdminController(
    @Value("\${admin.api.key}") private val adminApiKey: String,
    private val imageStorageService: ImageStorageService,
    private val hennaDesignService: HennaDesignService,
    private val invitationCardService: InvitationCardService,
    @Autowired(required = false) private val s3StorageService: S3StorageService? = null
) {

    private fun validateApiKey(apiKeyHeader: String?) {
        if (apiKeyHeader.isNullOrBlank() || apiKeyHeader != adminApiKey) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or missing X-Admin-Key header")
        }
    }

    private fun uploadImage(file: MultipartFile): String {
        if (file.isEmpty) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty")
        }
        val contentType = file.contentType ?: "image/jpeg"
        if (contentType !in setOf("image/jpeg", "image/png", "image/webp")) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported image format. Allowed formats: JPEG, PNG, WEBP")
        }

        return if (s3StorageService != null) {
            try {
                s3StorageService.upload(file.bytes, contentType, file.originalFilename)
            } catch (e: Exception) {
                throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "S3 upload failed: ${e.message}")
            }
        } else {
            // Local fallback
            try {
                val base64 = Base64.getEncoder().encodeToString(file.bytes)
                imageStorageService.store(base64, file.originalFilename)
            } catch (e: Exception) {
                throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Local upload fallback failed: ${e.message}")
            }
        }
    }

    @PostMapping(BackendRoutes.Admin.DESIGNS)
    fun createDesign(
        @RequestHeader("X-Admin-Key", required = false) apiKey: String?,
        @RequestParam("file") file: MultipartFile,
        @RequestParam("name") name: String,
        @RequestParam("price") price: Int,
        @RequestParam("complexity") complexity: String,
        @RequestParam("tags") tags: String
    ): ResponseEntity<HennaDesign> {
        validateApiKey(apiKey)

        val imageUrl = uploadImage(file)

        val design = HennaDesign(
            imageUrl = imageUrl,
            name = name,
            price = price,
            complexity = complexity,
            tags = tags
        )

        val savedDesign = hennaDesignService.saveDesign(design)
        return ResponseEntity.status(HttpStatus.CREATED).body(savedDesign)
    }

    @DeleteMapping(BackendRoutes.Admin.DESIGN_BY_ID)
    fun deleteDesign(
        @RequestHeader("X-Admin-Key", required = false) apiKey: String?,
        @PathVariable("id") id: Long
    ): ResponseEntity<Map<String, String>> {
        validateApiKey(apiKey)

        hennaDesignService.getDesignById(id)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Henna design not found")

        hennaDesignService.deleteDesign(id)
        return ResponseEntity.ok(mapOf("message" to "Henna design deleted successfully"))
    }

    @PostMapping(BackendRoutes.Admin.INVITATION_CARDS)
    fun createInvitationCard(
        @RequestHeader("X-Admin-Key", required = false) apiKey: String?,
        @RequestParam("file") file: MultipartFile,
        @RequestParam("name") name: String,
        @RequestParam("description", required = false) description: String?,
        @RequestParam("price") price: Int,
        @RequestParam("finish") finish: String,
        @RequestParam("printType") printType: String,
        @RequestParam("size") size: String,
        @RequestParam("material") material: String,
        @RequestParam("paperWeight") paperWeight: Int,
        @RequestParam("minOrderQuantity") minOrderQuantity: Int,
        @RequestParam("tags") tags: String
    ): ResponseEntity<InvitationCard> {
        validateApiKey(apiKey)

        val imageUrl = uploadImage(file)

        val card = InvitationCard(
            name = name,
            description = description,
            imageUrl = imageUrl,
            price = price,
            finish = finish.uppercase(),
            printType = printType.uppercase(),
            size = size,
            material = material.uppercase(),
            paperWeight = paperWeight,
            minOrderQuantity = minOrderQuantity,
            tags = tags
        )

        val savedCard = invitationCardService.saveCard(card)
        return ResponseEntity.status(HttpStatus.CREATED).body(savedCard)
    }

    @DeleteMapping(BackendRoutes.Admin.INVITATION_CARD_BY_ID)
    fun deleteInvitationCard(
        @RequestHeader("X-Admin-Key", required = false) apiKey: String?,
        @PathVariable("id") id: Long
    ): ResponseEntity<Map<String, String>> {
        validateApiKey(apiKey)

        invitationCardService.getCardById(id)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Invitation card not found")

        invitationCardService.deleteCard(id)
        return ResponseEntity.ok(mapOf("message" to "Invitation card deleted successfully"))
    }
}
