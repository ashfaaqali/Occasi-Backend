package com.occasi.application.controller

import com.occasi.application.config.TestFirebaseConfig
import com.occasi.application.model.HennaDesign
import com.occasi.application.model.InvitationCard
import com.occasi.application.repository.HennaDesignRepository
import com.occasi.application.repository.InvitationCardRepository
import com.occasi.application.service.S3StorageService
import io.kotest.core.spec.style.StringSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@Import(TestFirebaseConfig::class)
@AutoConfigureMockMvc
@Transactional
class AdminControllerTest : StringSpec() {

    override fun extensions() = listOf(SpringExtension)

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var hennaDesignRepository: HennaDesignRepository

    @Autowired
    lateinit var invitationCardRepository: InvitationCardRepository

    @Autowired
    lateinit var s3StorageService: S3StorageService

    private val validApiKey = "default_development_admin_key_123"

    init {
        "POST /admin/designs should fail with 401 when API key is missing or invalid" {
            val file = MockMultipartFile("file", "test.jpg", "image/jpeg", "dummy-image-bytes".toByteArray())

            // Missing key
            mockMvc.perform(
                multipart("/admin/designs")
                    .file(file)
                    .param("name", "Design Test")
                    .param("designType", "HAND")
                    .param("complexity", "Simple")
                    .param("tags", "TEST")
            ).andExpect(status().isUnauthorized)

            // Invalid key
            mockMvc.perform(
                multipart("/admin/designs")
                    .file(file)
                    .param("name", "Design Test")
                    .param("designType", "HAND")
                    .param("complexity", "Simple")
                    .param("tags", "TEST")
                    .header("X-Admin-Key", "wrong_key")
            ).andExpect(status().isUnauthorized)
        }

        "POST /admin/designs should succeed and save design with valid key" {
            whenever(s3StorageService.upload(any(), any(), anyOrNull()))
                .thenReturn("https://test-bucket.s3.amazonaws.com/designs/test.jpg")

            val file = MockMultipartFile("file", "test.png", "image/png", "dummy-image-bytes".toByteArray())

            val countBefore = hennaDesignRepository.count()

            mockMvc.perform(
                multipart("/admin/designs")
                    .file(file)
                    .param("name", "New Bridal Special")
                    .param("designType", "HAND")
                    .param("complexity", "Bridal")
                    .param("tags", "BRIDAL,ELEGANT")
                    .header("X-Admin-Key", validApiKey)
            ).andExpect(status().isCreated)
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("New Bridal Special"))
                .andExpect(jsonPath("$.designType").value("HAND"))
                .andExpect(jsonPath("$.complexity").value("Bridal"))
                .andExpect(jsonPath("$.tags").value("BRIDAL,ELEGANT"))
                .andExpect(jsonPath("$.imageUrl").exists())

            hennaDesignRepository.count() shouldBe (countBefore + 1)
        }

        "DELETE /admin/designs/{id} should delete design when key is valid" {
            val design = hennaDesignRepository.save(
                HennaDesign(
                    imageUrl = "http://example.com/img.jpg",
                    name = "To Be Deleted",
                    designType = com.occasi.application.model.DesignType.HAND,
                    complexity = "Simple",
                    tags = "TEMP"
                )
            )

            val designId = design.id!!
            hennaDesignRepository.findById(designId).isPresent shouldBe true

            // Without key
            mockMvc.perform(
                delete("/admin/designs/$designId")
            ).andExpect(status().isUnauthorized)

            // With valid key
            mockMvc.perform(
                delete("/admin/designs/$designId")
                    .header("X-Admin-Key", validApiKey)
            ).andExpect(status().isOk)

            hennaDesignRepository.findById(designId).isPresent shouldBe false
        }

        "POST /admin/invitation-cards should fail with 401 when API key is missing or invalid" {
            val file = MockMultipartFile("file", "card.jpg", "image/jpeg", "dummy-image-bytes".toByteArray())

            mockMvc.perform(
                multipart("/admin/invitation-cards")
                    .file(file)
                    .param("name", "Card Test")
                    .param("price", "100")
                    .param("finish", "MATTE")
                    .param("printType", "DIGITAL")
                    .param("size", "5x7")
                    .param("material", "CARDSTOCK")
                    .param("paperWeight", "300")
                    .param("minOrderQuantity", "50")
                    .param("tags", "TEST")
            ).andExpect(status().isUnauthorized)
        }

        "POST /admin/invitation-cards should succeed and save card with valid key" {
            whenever(s3StorageService.upload(any(), any(), anyOrNull()))
                .thenReturn("https://test-bucket.s3.amazonaws.com/invitation-cards/card.jpg")

            val file = MockMultipartFile("file", "card.png", "image/png", "dummy-image-bytes".toByteArray())

            val countBefore = invitationCardRepository.count()

            mockMvc.perform(
                multipart("/admin/invitation-cards")
                    .file(file)
                    .param("name", "Royal Wedding Card")
                    .param("description", "Luxury wedding card")
                    .param("price", "150")
                    .param("finish", "glossy")
                    .param("printType", "offset")
                    .param("size", "6x9")
                    .param("material", "velvet")
                    .param("paperWeight", "350")
                    .param("minOrderQuantity", "100")
                    .param("tags", "WEDDING,LUXURY")
                    .header("X-Admin-Key", validApiKey)
            ).andExpect(status().isCreated)
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Royal Wedding Card"))
                .andExpect(jsonPath("$.price").value(150))
                .andExpect(jsonPath("$.finish").value("GLOSSY"))
                .andExpect(jsonPath("$.printType").value("OFFSET"))
                .andExpect(jsonPath("$.material").value("VELVET"))
                .andExpect(jsonPath("$.imageUrl").exists())

            invitationCardRepository.count() shouldBe (countBefore + 1)
        }

        "DELETE /admin/invitation-cards/{id} should delete card when key is valid" {
            val card = invitationCardRepository.save(
                InvitationCard(
                    name = "Temporary Card",
                    imageUrl = "http://example.com/card.jpg",
                    price = 80,
                    finish = "MATTE",
                    printType = "DIGITAL",
                    size = "5x7",
                    material = "CARDSTOCK",
                    paperWeight = 300,
                    minOrderQuantity = 50,
                    tags = "TEMP"
                )
            )

            val cardId = card.id!!
            invitationCardRepository.findById(cardId).isPresent shouldBe true

            // Without key
            mockMvc.perform(
                delete("/admin/invitation-cards/$cardId")
            ).andExpect(status().isUnauthorized)

            // With valid key
            mockMvc.perform(
                delete("/admin/invitation-cards/$cardId")
                    .header("X-Admin-Key", validApiKey)
            ).andExpect(status().isOk)

            invitationCardRepository.findById(cardId).isPresent shouldBe false
        }
    }
}
