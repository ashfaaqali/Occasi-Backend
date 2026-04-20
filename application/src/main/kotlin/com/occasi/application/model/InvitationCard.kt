package com.occasi.application.model

import jakarta.persistence.*

@Entity
data class InvitationCard(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    var name: String,
    var description: String? = null,
    var imageUrl: String,
    var price: Int,
    var finish: String,                // MATTE, GLOSSY, TEXTURED, EMBOSSED, FOIL_STAMPED
    var printType: String,             // DIGITAL, OFFSET, SCREEN_PRINT, LETTERPRESS
    var size: String,                  // comma-separated available sizes e.g. "5×7 inches,6×9 inches,4×6 inches"
    var material: String,              // CARDSTOCK, COTTON, VELVET, ACRYLIC, WOOD
    var paperWeight: Int,              // GSM
    var minOrderQuantity: Int = 1,
    var tags: String = "",             // comma-separated
    var numberOfOrders: Int = 0,
    var averageRating: Double = 0.0,   // 0.0–5.0
    var reviewCount: Int = 0
)
