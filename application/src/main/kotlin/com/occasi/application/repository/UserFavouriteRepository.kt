package com.occasi.application.repository

import com.occasi.application.model.ItemType
import com.occasi.application.model.UserFavourite
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserFavouriteRepository : JpaRepository<UserFavourite, Long> {
    fun findByUserIdOrderByCreatedAtDesc(userId: Long): List<UserFavourite>
    fun findByUserIdAndItemTypeOrderByCreatedAtDesc(userId: Long, itemType: ItemType): List<UserFavourite>
    fun findByUserIdAndItemIdAndItemType(userId: Long, itemId: Long, itemType: ItemType): UserFavourite?
    fun deleteByUserIdAndItemIdAndItemType(userId: Long, itemId: Long, itemType: ItemType)
}
