package com.occasi.application.model

import jakarta.persistence.*

@Entity
data class InvitationCard(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    var imageUrl: String,
    var price: Int,
    var occasionCategory: String,  // "WEDDING", "ENGAGEMENT", "MEHNDI", "RECEPTION"
    var priceRange: String,        // "UNDER_100", "RANGE_100_200", "ABOVE_200"

    // Card Physical Properties
    var width: Double,             // Width in inches
    var height: Double,            // Height in inches
    var paperType: String,         // "MATTE", "GLOSSY", "TEXTURED", "HANDMADE", "RECYCLED"
    var paperWeight: Int,          // GSM (grams per square meter)
    var material: String,          // "CARDSTOCK", "COTTON", "VELVET", "ACRYLIC", "WOOD"
    var printQuality: String,      // "STANDARD", "PREMIUM", "LUXURY"

    // Additional Details
    var name: String,
    var description: String? = null,
    var isCustomizable: Boolean = false,
    var minOrderQuantity: Int = 1,
    var numberOfOrders: Int = 0
)
