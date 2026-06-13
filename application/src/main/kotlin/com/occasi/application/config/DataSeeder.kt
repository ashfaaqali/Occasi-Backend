package com.occasi.application.config

import com.occasi.application.model.*
import com.occasi.application.repository.HennaDesignRepository
import com.occasi.application.repository.InvitationCardRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import kotlin.random.Random

@Configuration
@Profile("dev")
class DataSeeder {

    // Invitation Card Data
    private val occasionCategories = listOf("WEDDING", "ENGAGEMENT", "MEHNDI", "RECEPTION")
    private val invitationTags = listOf("Wedding", "Engagement", "Mehndi", "Reception", "Elegant", "Traditional", "Modern", "Floral", "Minimalist", "Luxury")
    private val finishes = listOf("MATTE", "GLOSSY", "TEXTURED", "EMBOSSED", "FOIL_STAMPED")
    private val printTypes = listOf("DIGITAL", "OFFSET", "SCREEN_PRINT", "LETTERPRESS")
    private val materials = listOf("CARDSTOCK", "COTTON", "VELVET", "ACRYLIC", "WOOD")
    private val sizes = listOf("5×7 inches", "6×9 inches", "4×6 inches")

    private val cardNames = mapOf(
        "WEDDING" to listOf(
            "Royal Elegance", "Golden Mandala", "Floral Dreams", "Classic Paisley", "Regal Charm",
            "Eternal Love", "Divine Union", "Sacred Vows", "Timeless Beauty", "Majestic Bloom"
        ),
        "ENGAGEMENT" to listOf(
            "Promise Ring", "Love Story", "Sweet Beginnings", "Heart to Heart", "Forever Yours",
            "Sparkling Moments", "New Chapter", "Ring Ceremony", "Blissful Bond", "Together Forever"
        ),
        "MEHNDI" to listOf(
            "Henna Night", "Mehndi Magic", "Traditional Touch", "Colorful Celebration", "Festive Vibes",
            "Sangeet Special", "Dancing Hands", "Bridal Henna", "Mehndi Moments", "Artistic Swirls"
        ),
        "RECEPTION" to listOf(
            "Grand Celebration", "Evening Elegance", "Party Perfect", "Glamour Night", "Starlit Soiree",
            "Crystal Clear", "Champagne Toast", "Midnight Magic", "Velvet Dreams", "Golden Hour"
        )
    )

    private fun generateInvitationCard(index: Int): InvitationCard {
        val occasion = occasionCategories[index % occasionCategories.size]
        val material = materials.random()
        val finish = finishes.random()
        val printType = printTypes.random()
        // All cards offer all available sizes
        val availableSizes = sizes.joinToString(",")

        // Price based on material
        val price = when (material) {
            "CARDSTOCK" -> Random.nextInt(50, 100)
            "COTTON" -> Random.nextInt(80, 150)
            "VELVET" -> Random.nextInt(150, 250)
            "ACRYLIC" -> Random.nextInt(200, 350)
            "WOOD" -> Random.nextInt(250, 400)
            else -> Random.nextInt(50, 150)
        }

        val names = cardNames[occasion] ?: listOf("Beautiful Card")
        val name = names[index % names.size]

        val paperWeight = when (finish) {
            "MATTE", "GLOSSY" -> listOf(250, 300, 350).random()
            "TEXTURED" -> listOf(280, 320, 380).random()
            "EMBOSSED" -> listOf(300, 350, 400).random()
            "FOIL_STAMPED" -> listOf(320, 370, 420).random()
            else -> 300
        }

        return InvitationCard(
            imageUrl = "https://picsum.photos/id/${200 + index}/400/500",
            price = price,
            finish = finish,
            printType = printType,
            size = availableSizes,
            material = material,
            paperWeight = paperWeight,
            name = name,
            description = "Beautiful $name invitation card for your special $occasion celebration. Made with premium $material and $finish finish.",
            minOrderQuantity = listOf(10, 25, 50, 100).random(),
            numberOfOrders = Random.nextInt(0, 500),
            tags = (listOf(occasion) + invitationTags.shuffled().take(Random.nextInt(1, 4))).distinct().joinToString(","),
            averageRating = 0.0,
            reviewCount = 0
        )
    }

    @Bean
    fun initInvitationCards(repository: InvitationCardRepository) = CommandLineRunner {
        if (repository.count() > 0) return@CommandLineRunner

        val cards = (1..60).map { generateInvitationCard(it) }
        repository.saveAll(cards)
        println("✓ Seeded ${cards.size} invitation cards")
    }

    // Henna Design Data
    private val designNames = listOf(
        "Bridal Full Hand", "Arabic Vine", "Peacock Motif", "Mandala Circle",
        "Floral Trail", "Rajasthani Royal", "Moroccan Lattice", "Lotus Bloom",
        "Paisley Cascade", "Mughal Jali", "Finger Tip Elegance", "Back Hand Swirl",
        "Dulhan Special", "Minimalist Leaf", "Indo-Arabic Fusion", "Tikki Round",
        "Bail Pattern", "Elephant Motif", "Jaali Net", "Rose Garden",
        "Butterfly Wings", "Geometric Modern", "Traditional Bangle", "Wrist Cuff",
        "Feet Anklet Design", "Palm Chakra", "Shoulder Drape", "Arm Band",
        "Festive Diwali", "Eid Crescent"
    )

    private val designTags = listOf("BRIDAL", "ARABIC", "INDIAN", "MOROCCAN", "MINIMALIST", "TRADITIONAL", "MODERN", "FLORAL", "GEOMETRIC", "FESTIVE")
    private val complexities = listOf("Simple", "Mid", "Complex", "Bridal")

    // Curated mehndi/henna image URLs from Unsplash
    private val mehndiImageUrls = listOf(
        "https://images.unsplash.com/photo-1595526051245-4506e0005bd0?w=400&h=500&fit=crop",
        "https://images.unsplash.com/photo-1600003014755-ba31aa59c4b6?w=400&h=500&fit=crop",
        "https://images.unsplash.com/photo-1591122947157-26bad3a117d2?w=400&h=500&fit=crop",
        "https://images.unsplash.com/photo-1609682698530-ef1e442a9449?w=400&h=500&fit=crop",
        "https://images.unsplash.com/photo-1615196534498-d1b1f1c88b3a?w=400&h=500&fit=crop",
        "https://images.unsplash.com/photo-1583089892943-e02e5b017b6a?w=400&h=500&fit=crop",
        "https://images.unsplash.com/photo-1570172619684-9bfb2895e72d?w=400&h=500&fit=crop",
        "https://images.unsplash.com/photo-1611516491426-03025e6043c8?w=400&h=500&fit=crop",
        "https://images.unsplash.com/photo-1596455607563-ad6193f76b17?w=400&h=500&fit=crop",
        "https://images.unsplash.com/photo-1617201929338-c0e4c1a8e3b5?w=400&h=500&fit=crop"
    )

    private fun generateDesign(index: Int): HennaDesign {
        val name = designNames[index % designNames.size]
        val complexity = complexities[index % complexities.size]
        val designType = if (index % 2 == 0) DesignType.HAND else DesignType.FEET
        val tagCount = Random.nextInt(2, 5)
        val tags = designTags.shuffled().take(tagCount).joinToString(",")
        val imageUrl = mehndiImageUrls[index % mehndiImageUrls.size]

        return HennaDesign(
            imageUrl = imageUrl,
            name = name,
            designType = designType,
            complexity = complexity,
            tags = tags,
            likes = Random.nextInt(0, 300),
            numberOfPeopleBooked = Random.nextInt(0, 150)
        )
    }

    @Bean
    fun initDesigns(
        designRepository: HennaDesignRepository
    ) = CommandLineRunner {
        if (designRepository.count() > 0) return@CommandLineRunner

        val designs = (0 until 30).map { index -> generateDesign(index) }
        designRepository.saveAll(designs)
        println("✓ Seeded ${designs.size} henna designs")
    }
}
