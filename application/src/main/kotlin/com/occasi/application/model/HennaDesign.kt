package com.occasi.application.model

import jakarta.persistence.*

@Entity
data class HennaDesign(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    var imageUrl: String,
    var name: String = "",           // Design name for display
    var price: Int,
    var complexity: String,          // "Simple", "Mid", "Complex"
    var tags: String = "",           // Comma-separated tags e.g. "BRIDAL,ARABIC,WEDDING"
    var likes: Int = 0,
    var numberOfPeopleBooked: Int = 0
) {
    init {
        require(name.isNotBlank()) { "Design name must not be empty" }
        require(complexity in listOf("Simple", "Mid", "Complex", "Bridal")) { "Complexity must be Simple, Mid, Complex, or Bridal" }
        require(tags.isNotBlank()) { "Tags must not be empty" }
        tags.split(",").forEach { tag ->
            require(tag.trim().isNotEmpty()) { "Each tag must be non-empty after trimming" }
        }
    }
}
