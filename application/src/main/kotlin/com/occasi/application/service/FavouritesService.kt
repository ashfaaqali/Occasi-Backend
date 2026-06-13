package com.occasi.application.service

import com.occasi.application.dto.FavouriteItemResponse
import com.occasi.application.model.ItemType
import com.occasi.application.model.UserFavourite
import com.occasi.application.model.ComplexityTier
import com.occasi.application.repository.ArtistPricingRepository
import com.occasi.application.repository.HennaArtistRepository
import com.occasi.application.repository.HennaDesignRepository
import com.occasi.application.repository.InvitationCardRepository
import com.occasi.application.repository.UserFavouriteRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FavouritesService(
    private val favouriteRepository: UserFavouriteRepository,
    private val hennaArtistRepository: HennaArtistRepository,
    private val hennaDesignRepository: HennaDesignRepository,
    private val invitationCardRepository: InvitationCardRepository,
    private val artistPricingRepository: ArtistPricingRepository
) {

    fun addFavourite(userId: Long, itemId: Long, itemType: ItemType): UserFavourite {
        val existing = favouriteRepository.findByUserIdAndItemIdAndItemType(userId, itemId, itemType)
        if (existing != null) return existing
        return favouriteRepository.save(UserFavourite(userId = userId, itemId = itemId, itemType = itemType))
    }

    @Transactional
    fun removeFavourite(userId: Long, itemId: Long, itemType: ItemType) {
        favouriteRepository.deleteByUserIdAndItemIdAndItemType(userId, itemId, itemType)
    }

    fun getFavourites(userId: Long, itemType: ItemType?): List<FavouriteItemResponse> {
        val favourites = if (itemType != null) {
            favouriteRepository.findByUserIdAndItemTypeOrderByCreatedAtDesc(userId, itemType)
        } else {
            favouriteRepository.findByUserIdOrderByCreatedAtDesc(userId)
        }
        return favourites.mapNotNull { fav -> toResponse(fav) }
    }

    private fun toResponse(fav: UserFavourite): FavouriteItemResponse? {
        return when (fav.itemType) {
            ItemType.ARTIST -> hennaArtistRepository.findById(fav.itemId).orElse(null)?.let {
                FavouriteItemResponse(
                    itemId = it.id!!,
                    itemType = "ARTIST",
                    createdAt = fav.createdAt.toString(),
                    name = it.name,
                    imageUrl = it.coverImage ?: "",
                    price = it.startingPrice
                )
            }
            ItemType.DESIGN -> hennaDesignRepository.findById(fav.itemId).orElse(null)?.let {
                val minPrice = artistPricingRepository.findByComplexityAndDesignType(
                    ComplexityTier.valueOf(it.complexity.uppercase()),
                    it.designType
                ).map { p -> p.price }.minOrNull() ?: 0
                FavouriteItemResponse(
                    itemId = it.id!!,
                    itemType = "DESIGN",
                    createdAt = fav.createdAt.toString(),
                    name = it.name,
                    imageUrl = it.imageUrl,
                    price = minPrice
                )
            }
            ItemType.CARD -> invitationCardRepository.findById(fav.itemId).orElse(null)?.let {
                FavouriteItemResponse(
                    itemId = it.id!!,
                    itemType = "CARD",
                    createdAt = fav.createdAt.toString(),
                    name = it.name,
                    imageUrl = it.imageUrl,
                    price = it.price
                )
            }
        }
    }
}
