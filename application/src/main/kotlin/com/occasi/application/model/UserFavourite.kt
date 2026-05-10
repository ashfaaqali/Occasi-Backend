package com.occasi.application.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "user_favourites",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "item_id", "item_type"])]
)
data class UserFavourite(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "user_id", nullable = false)
    var userId: Long,

    @Column(name = "item_id", nullable = false)
    var itemId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false)
    var itemType: ItemType,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
)
