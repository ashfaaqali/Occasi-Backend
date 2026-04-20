package com.occasi.application

import com.occasi.application.model.InvitationCard
import com.occasi.application.repository.InvitationCardRepository
import com.occasi.application.service.InvitationCardService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.PropertyTesting
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import org.mockito.kotlin.*

// Feature: invitation-cards-ordering
class InvitationCardServicePropertyTest : StringSpec({

    beforeSpec {
        PropertyTesting.defaultIterationCount = 100
    }

    fun makeCard(
        price: Int = 100,
        minOrderQuantity: Int = 1,
        material: String = "CARDSTOCK",
        finish: String = "MATTE"
    ) = InvitationCard(
        id = null,
        name = "Test Card",
        imageUrl = "https://example.com/img.png",
        price = price,
        finish = finish,
        printType = "DIGITAL",
        size = "5×7 inches",
        material = material,
        paperWeight = 300,
        minOrderQuantity = minOrderQuantity
    )

    // Property 3: InvitationCard positive field validation
    // **Validates: Requirements 1.10, 1.11**
    "Property 3: saveCard accepts iff price > 0 and minOrderQuantity > 0" {
        val arbPrice = Arb.int(-100..100)
        val arbMinQty = Arb.int(-100..100)

        checkAll(arbPrice, arbMinQty) { price, minQty ->
            val repository: InvitationCardRepository = mock()
            doAnswer { it.arguments[0] }.whenever(repository).save(any())
            val service = InvitationCardService(repository)

            val card = makeCard(price = price, minOrderQuantity = minQty)
            val shouldAccept = price > 0 && minQty > 0

            if (shouldAccept) {
                val result = service.saveCard(card)
                result.price shouldBe price
                result.minOrderQuantity shouldBe minQty
            } else {
                shouldThrow<IllegalArgumentException> {
                    service.saveCard(card)
                }
            }
        }
    }

    // Property 8: Card filter correctness
    // **Validates: Requirements 2.4, 2.5**
    "Property 8: filtering by material returns exactly matching cards" {
        val materials = listOf("CARDSTOCK", "COTTON", "VELVET", "ACRYLIC", "WOOD")
        val arbMaterial = Arb.element(materials)

        checkAll(arbMaterial) { filterMaterial ->
            val repository: InvitationCardRepository = mock()

            // Build a set of cards with various materials
            val allCards = materials.flatMapIndexed { idx, mat ->
                listOf(
                    makeCard(material = mat).copy(id = (idx * 2 + 1).toLong()),
                    makeCard(material = mat).copy(id = (idx * 2 + 2).toLong())
                )
            }

            val expectedCards = allCards.filter { it.material == filterMaterial }

            whenever(repository.findByMaterial(filterMaterial)).thenReturn(expectedCards)

            val service = InvitationCardService(repository)
            val result = service.getCardsByMaterial(filterMaterial)

            // All returned cards match the filter
            result.all { it.material == filterMaterial } shouldBe true
            // No matching cards are excluded
            result shouldContainExactlyInAnyOrder expectedCards
        }
    }

    "Property 8: filtering by finish returns exactly matching cards" {
        val finishes = listOf("MATTE", "GLOSSY", "TEXTURED", "EMBOSSED", "FOIL_STAMPED")
        val arbFinish = Arb.element(finishes)

        checkAll(arbFinish) { filterFinish ->
            val repository: InvitationCardRepository = mock()

            // Build a set of cards with various finishes
            val allCards = finishes.flatMapIndexed { idx, fin ->
                listOf(
                    makeCard(finish = fin).copy(id = (idx * 2 + 1).toLong()),
                    makeCard(finish = fin).copy(id = (idx * 2 + 2).toLong())
                )
            }

            val expectedCards = allCards.filter { it.finish == filterFinish }

            whenever(repository.findByFinish(filterFinish)).thenReturn(expectedCards)

            val service = InvitationCardService(repository)
            val result = service.getCardsByFinish(filterFinish)

            // All returned cards match the filter
            result.all { it.finish == filterFinish } shouldBe true
            // No matching cards are excluded
            result shouldContainExactlyInAnyOrder expectedCards
        }
    }
})
