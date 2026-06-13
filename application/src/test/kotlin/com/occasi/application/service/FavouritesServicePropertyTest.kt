package com.occasi.application.service

import com.occasi.application.model.*
import com.occasi.application.repository.HennaArtistRepository
import com.occasi.application.repository.HennaDesignRepository
import com.occasi.application.repository.InvitationCardRepository
import com.occasi.application.repository.UserFavouriteRepository
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import org.mockito.kotlin.*
import java.time.LocalDateTime
import java.util.Optional

// Feature: saved-favourites, Property 1: Add favourite is idempotent
// Feature: saved-favourites, Property 2: Add/Remove round-trip
// Feature: saved-favourites, Property 3: Favourites list sorted descending by created_at
// Feature: saved-favourites, Property 4: Type filter returns only matching items
import com.occasi.application.repository.ArtistPricingRepository

class FavouritesServicePropertyTest : StringSpec({

    // --- Shared generators ---
    val arbUserId = Arb.long(1L..10_000L)
    val arbItemId = Arb.long(1L..10_000L)
    val arbItemType = Arb.enum<ItemType>()
    val arbPrice = Arb.int(100..50_000)
    val arbName = Arb.string(3..30).filter { it.isNotBlank() }

    fun makeArtist(id: Long, name: String, price: Int) = HennaArtist(
        id = id, name = name, email = "artist@test.com",
        mobileNumber = "9876543210", cityName = "Mumbai", location = "Andheri",
        coverImage = "http://img.png", startingPrice = price
    )

    fun makeDesign(id: Long, name: String, price: Int) = HennaDesign(
        id = id, imageUrl = "http://design.png", name = name,
        complexity = "Simple", tags = "BRIDAL"
    )

    fun makeCard(id: Long, name: String, price: Int) = InvitationCard(
        id = id, name = name, imageUrl = "http://card.png", price = price,
        finish = "MATTE", printType = "DIGITAL", size = "5×7 inches",
        material = "CARDSTOCK", paperWeight = 300, description = "Test card"
    )

    fun buildService(
        favouriteRepo: UserFavouriteRepository = mock(),
        artistRepo: HennaArtistRepository = mock(),
        designRepo: HennaDesignRepository = mock(),
        cardRepo: InvitationCardRepository = mock(),
        artistPricingRepo: ArtistPricingRepository = mock()
    ) = FavouritesService(favouriteRepo, artistRepo, designRepo, cardRepo, artistPricingRepo)

    // **Validates: Requirements 1.2, 1.3**
    // Feature: saved-favourites, Property 1: Add favourite is idempotent
    "addFavourite is idempotent - calling twice results in exactly one record and both return same record" {
        checkAll(PropTestConfig(iterations = 100), arbUserId, arbItemId, arbItemType) { userId, itemId, itemType ->
            val favouriteRepo: UserFavouriteRepository = mock()

            // First call: no existing record, save creates one
            val savedFavourite = UserFavourite(
                id = 1L, userId = userId, itemId = itemId, itemType = itemType,
                createdAt = LocalDateTime.now()
            )

            // First call: findByUserIdAndItemIdAndItemType returns null (no existing)
            // Second call: findByUserIdAndItemIdAndItemType returns the saved record
            whenever(favouriteRepo.findByUserIdAndItemIdAndItemType(userId, itemId, itemType))
                .thenReturn(null)       // first call
                .thenReturn(savedFavourite) // second call

            doAnswer { savedFavourite }.whenever(favouriteRepo).save(any())

            val service = buildService(favouriteRepo = favouriteRepo)

            val result1 = service.addFavourite(userId, itemId, itemType)
            val result2 = service.addFavourite(userId, itemId, itemType)

            // Both calls return the same record
            result1 shouldBe result2
            result1.userId shouldBe userId
            result1.itemId shouldBe itemId
            result1.itemType shouldBe itemType

            // save is called only once (first call), second call returns existing
            verify(favouriteRepo, times(1)).save(any())
        }
    }

    // **Validates: Requirements 2.1, 2.2**
    // Feature: saved-favourites, Property 2: Add/Remove round-trip
    "add then remove results in favourite no longer existing" {
        checkAll(PropTestConfig(iterations = 100), arbUserId, arbItemId, arbItemType) { userId, itemId, itemType ->
            val favouriteRepo: UserFavouriteRepository = mock()
            val artistRepo: HennaArtistRepository = mock()
            val designRepo: HennaDesignRepository = mock()
            val cardRepo: InvitationCardRepository = mock()

            val savedFavourite = UserFavourite(
                id = 1L, userId = userId, itemId = itemId, itemType = itemType,
                createdAt = LocalDateTime.now()
            )

            // Setup for addFavourite
            whenever(favouriteRepo.findByUserIdAndItemIdAndItemType(userId, itemId, itemType))
                .thenReturn(null)  // for add
            doAnswer { savedFavourite }.whenever(favouriteRepo).save(any())

            // Setup for getFavourites after removal - returns empty list
            whenever(favouriteRepo.findByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(emptyList())

            val service = buildService(favouriteRepo, artistRepo, designRepo, cardRepo)

            // Add the favourite
            val added = service.addFavourite(userId, itemId, itemType)
            added.userId shouldBe userId
            added.itemId shouldBe itemId
            added.itemType shouldBe itemType

            // Remove the favourite
            service.removeFavourite(userId, itemId, itemType)

            // Verify delete was called
            verify(favouriteRepo).deleteByUserIdAndItemIdAndItemType(userId, itemId, itemType)

            // After removal, getFavourites should not contain the item
            val remaining = service.getFavourites(userId, null)
            remaining.shouldBeEmpty()
        }
    }

    // **Validates: Requirements 2.3**
    // Feature: saved-favourites, Property 3: Favourites list sorted descending by created_at
    "getFavourites returns items sorted by created_at descending" {
        val arbCount = Arb.int(2..10)

        checkAll(PropTestConfig(iterations = 100), arbUserId, arbCount) { userId, count ->
            val favouriteRepo: UserFavouriteRepository = mock()
            val artistRepo: HennaArtistRepository = mock()
            val designRepo: HennaDesignRepository = mock()
            val cardRepo: InvitationCardRepository = mock()

            // Generate N favourites with distinct createdAt values in random order
            val baseTime = LocalDateTime.of(2024, 1, 1, 0, 0)
            val favourites = (1..count).map { i ->
                UserFavourite(
                    id = i.toLong(),
                    userId = userId,
                    itemId = i.toLong(),
                    itemType = ItemType.ARTIST,
                    createdAt = baseTime.plusMinutes(i.toLong())
                )
            }

            // Repository returns them sorted descending (as per the query method name)
            val sortedDesc = favourites.sortedByDescending { it.createdAt }
            whenever(favouriteRepo.findByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(sortedDesc)

            // Setup artist repo to return valid artists for each item
            sortedDesc.forEach { fav ->
                val artist = makeArtist(fav.itemId, "Artist ${fav.itemId}", 1000)
                whenever(artistRepo.findById(fav.itemId)).thenReturn(Optional.of(artist))
            }

            val service = buildService(favouriteRepo, artistRepo, designRepo, cardRepo)
            val results = service.getFavourites(userId, null)

            results shouldHaveSize count

            // Verify descending order: each item's createdAt >= next item's createdAt
            for (i in 0 until results.size - 1) {
                val current = results[i].createdAt
                val next = results[i + 1].createdAt
                (current >= next) shouldBe true
            }
        }
    }

    // **Validates: Requirements 2.4**
    // Feature: saved-favourites, Property 4: Type filter returns only matching items
    "type filter returns only matching items and all of them" {
        checkAll(PropTestConfig(iterations = 100), arbUserId, arbItemType) { userId, filterType ->
            val favouriteRepo: UserFavouriteRepository = mock()
            val artistRepo: HennaArtistRepository = mock()
            val designRepo: HennaDesignRepository = mock()
            val cardRepo: InvitationCardRepository = mock()

            val baseTime = LocalDateTime.of(2024, 1, 1, 0, 0)

            // Create favourites of mixed types
            val allFavourites = ItemType.entries.flatMapIndexed { typeIdx, type ->
                (1..3).map { i ->
                    val itemId = (typeIdx * 100 + i).toLong()
                    UserFavourite(
                        id = itemId,
                        userId = userId,
                        itemId = itemId,
                        itemType = type,
                        createdAt = baseTime.plusMinutes(itemId)
                    )
                }
            }

            // Filter to only the matching type, sorted descending
            val matchingFavourites = allFavourites
                .filter { it.itemType == filterType }
                .sortedByDescending { it.createdAt }

            whenever(favouriteRepo.findByUserIdAndItemTypeOrderByCreatedAtDesc(userId, filterType))
                .thenReturn(matchingFavourites)

            // Setup entity repos for each matching favourite
            matchingFavourites.forEach { fav ->
                when (fav.itemType) {
                    ItemType.ARTIST -> {
                        val artist = makeArtist(fav.itemId, "Artist ${fav.itemId}", 1000)
                        whenever(artistRepo.findById(fav.itemId)).thenReturn(Optional.of(artist))
                    }
                    ItemType.DESIGN -> {
                        val design = makeDesign(fav.itemId, "Design ${fav.itemId}", 2000)
                        whenever(designRepo.findById(fav.itemId)).thenReturn(Optional.of(design))
                    }
                    ItemType.CARD -> {
                        val card = makeCard(fav.itemId, "Card ${fav.itemId}", 3000)
                        whenever(cardRepo.findById(fav.itemId)).thenReturn(Optional.of(card))
                    }
                }
            }

            val service = buildService(favouriteRepo, artistRepo, designRepo, cardRepo)
            val results = service.getFavourites(userId, filterType)

            // All results should match the filter type
            results.forEach { it.itemType shouldBe filterType.name }

            // Should return all matching items (3 per type)
            results shouldHaveSize matchingFavourites.size

            // Should contain all matching item IDs
            val resultItemIds = results.map { it.itemId }.toSet()
            val expectedItemIds = matchingFavourites.map { it.itemId }.toSet()
            resultItemIds shouldContainAll expectedItemIds
        }
    }
})
