package com.occasi.application.config

import com.occasi.application.model.*
import com.occasi.application.repository.ArtistPricingRepository
import com.occasi.application.repository.HennaArtistRepository
import com.occasi.application.repository.HennaDesignRepository
import com.occasi.application.repository.InvitationCardRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import kotlin.random.Random

@Configuration
class DataSeeder {

    // Invitation Card Data
    private val occasionCategories = listOf("WEDDING", "ENGAGEMENT", "MEHNDI", "RECEPTION")
    private val invitationTags = listOf("Wedding", "Engagement", "Mehndi", "Reception", "Elegant", "Traditional", "Modern", "Floral", "Minimalist", "Luxury")
    private val paperTypes = listOf("MATTE", "GLOSSY", "TEXTURED", "HANDMADE", "RECYCLED")
    private val materials = listOf("CARDSTOCK", "COTTON", "VELVET", "ACRYLIC", "WOOD")
    private val printQualities = listOf("STANDARD", "PREMIUM", "LUXURY")

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

    private fun getPriceRange(price: Int): String {
        return when {
            price < 100 -> "UNDER_100"
            price <= 200 -> "RANGE_100_200"
            else -> "ABOVE_200"
        }
    }

    private fun generateInvitationCard(index: Int): InvitationCard {
        val occasion = occasionCategories[index % occasionCategories.size]
        val paperType = paperTypes.random()
        val material = materials.random()
        val printQuality = printQualities.random()

        // Price based on material and print quality
        val basePrice = when (material) {
            "CARDSTOCK" -> Random.nextInt(50, 100)
            "COTTON" -> Random.nextInt(80, 150)
            "VELVET" -> Random.nextInt(150, 250)
            "ACRYLIC" -> Random.nextInt(200, 350)
            "WOOD" -> Random.nextInt(250, 400)
            else -> Random.nextInt(50, 150)
        }

        val qualityMultiplier = when (printQuality) {
            "STANDARD" -> 1.0
            "PREMIUM" -> 1.3
            "LUXURY" -> 1.6
            else -> 1.0
        }

        val price = (basePrice * qualityMultiplier).toInt()
        val priceRange = getPriceRange(price)

        val names = cardNames[occasion] ?: listOf("Beautiful Card")
        val name = names[index % names.size]

        // Standard card dimensions (in inches)
        val dimensions = listOf(
            5.0 to 7.0,   // Standard
            4.0 to 6.0,   // Compact
            5.5 to 8.5,   // Large
            4.25 to 5.5   // A2 size
        )
        val (width, height) = dimensions.random()

        val paperWeight = when (paperType) {
            "MATTE", "GLOSSY" -> listOf(250, 300, 350).random()
            "TEXTURED" -> listOf(280, 320, 380).random()
            "HANDMADE" -> listOf(200, 250, 300).random()
            "RECYCLED" -> listOf(220, 270, 320).random()
            else -> 300
        }

        return InvitationCard(
            imageUrl = "https://picsum.photos/id/${200 + index}/400/500",
            price = price,
            priceRange = priceRange,
            width = width,
            height = height,
            paperType = paperType,
            paperWeight = paperWeight,
            material = material,
            printQuality = printQuality,
            name = name,
            description = "Beautiful $name invitation card for your special $occasion celebration. Made with premium $material and $paperType finish.",
            isCustomizable = Random.nextBoolean(),
            minOrderQuantity = listOf(10, 25, 50, 100).random(),
            numberOfOrders = Random.nextInt(0, 500),
            tags = (listOf(occasion) + invitationTags.shuffled().take(Random.nextInt(1, 4))).distinct().joinToString(",")
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

    // Henna Artist Data
    private data class ArtistSeed(
        val name: String,
        val email: String,
        val mobile: String,
        val city: String,
        val location: String,
        val rating: Short,
        val reviews: Int,
        val coverImage: String,
        val pricing: Map<ComplexityTier, Int>
    )

    private val artistSeeds = listOf(
        ArtistSeed("Priya Sharma", "priya@occasi.com", "9876543210", "Mumbai", "Andheri West, Mumbai", 5, 120,
            "https://images.unsplash.com/photo-1594744803329-e58b31239f85?w=400&h=400&fit=crop",
            mapOf(ComplexityTier.SIMPLE to 350, ComplexityTier.MID to 900, ComplexityTier.COMPLEX to 2500, ComplexityTier.BRIDAL to 6000)),
        ArtistSeed("Fatima Khan", "fatima@occasi.com", "9876543211", "Delhi", "Lajpat Nagar, Delhi", 5, 95,
            "https://images.unsplash.com/photo-1583089892943-e02e5b017b6a?w=400&h=400&fit=crop",
            mapOf(ComplexityTier.SIMPLE to 400, ComplexityTier.MID to 1000, ComplexityTier.COMPLEX to 2800, ComplexityTier.BRIDAL to 7000)),
        ArtistSeed("Ananya Patel", "ananya@occasi.com", "9876543212", "Jaipur", "Malviya Nagar, Jaipur", 4, 78,
            "https://images.unsplash.com/photo-1570172619684-9bfb2895e72d?w=400&h=400&fit=crop",
            mapOf(ComplexityTier.SIMPLE to 300, ComplexityTier.MID to 800, ComplexityTier.COMPLEX to 2000, ComplexityTier.BRIDAL to 5000)),
        ArtistSeed("Meera Reddy", "meera@occasi.com", "9876543213", "Hyderabad", "Banjara Hills, Hyderabad", 4, 62,
            "https://images.unsplash.com/photo-1611516491426-03025e6043c8?w=400&h=400&fit=crop",
            mapOf(ComplexityTier.SIMPLE to 450, ComplexityTier.MID to 1100, ComplexityTier.COMPLEX to 3000, ComplexityTier.BRIDAL to 7500)),
        ArtistSeed("Zara Sheikh", "zara@occasi.com", "9876543214", "Lucknow", "Hazratganj, Lucknow", 5, 140,
            "https://images.unsplash.com/photo-1596455607563-ad6193f76b17?w=400&h=400&fit=crop",
            mapOf(ComplexityTier.SIMPLE to 500, ComplexityTier.MID to 1200, ComplexityTier.COMPLEX to 2800, ComplexityTier.BRIDAL to 8000))
    )

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
        val price = when (complexity) {
            "Simple" -> Random.nextInt(200, 500)
            "Mid" -> Random.nextInt(500, 1500)
            "Complex" -> Random.nextInt(1500, 5000)
            else -> Random.nextInt(300, 1000)
        }
        val tagCount = Random.nextInt(2, 5)
        val tags = designTags.shuffled().take(tagCount).joinToString(",")
        val imageUrl = mehndiImageUrls[index % mehndiImageUrls.size]

        return HennaDesign(
            imageUrl = imageUrl,
            name = name,
            price = price,
            complexity = complexity,
            tags = tags,
            likes = Random.nextInt(0, 300),
            numberOfPeopleBooked = Random.nextInt(0, 150)
        )
    }

    @Bean
    fun initDesignsAndArtists(
        designRepository: HennaDesignRepository,
        artistRepository: HennaArtistRepository,
        artistPricingRepository: ArtistPricingRepository
    ) = CommandLineRunner {
        if (designRepository.count() > 0) return@CommandLineRunner

        // 1. Seed artists
        val savedArtists = if (artistRepository.count() == 0L) {
            val artists = artistSeeds.map { seed ->
                HennaArtist(
                    name = seed.name,
                    email = seed.email,
                    mobileNumber = seed.mobile,
                    cityName = seed.city,
                    location = seed.location,
                    rating = seed.rating,
                    reviews = seed.reviews,
                    coverImage = seed.coverImage
                )
            }
            artistRepository.saveAll(artists).also {
                println("✓ Seeded ${it.size} henna artists")
            }
        } else {
            artistRepository.findAll()
        }

        // 2. Seed designs and link to artists (round-robin)
        val designs = (0 until 30).map { index ->
            val design = generateDesign(index)
            design.artist = savedArtists[index % savedArtists.size]
            design
        }
        designRepository.saveAll(designs)
        println("✓ Seeded ${designs.size} henna designs")

        // 3. Seed ArtistPricing rows for each artist
        if (artistPricingRepository.count() == 0L) {
            val pricingRows = savedArtists.flatMap { artist ->
                val seed = artistSeeds.find { it.email == artist.email }
                val pricing = seed?.pricing ?: mapOf(
                    ComplexityTier.SIMPLE to 350,
                    ComplexityTier.MID to 900,
                    ComplexityTier.COMPLEX to 2500,
                    ComplexityTier.BRIDAL to 6000
                )
                pricing.map { (tier, price) ->
                    ArtistPricing(artist = artist, complexity = tier, price = price)
                }
            }
            artistPricingRepository.saveAll(pricingRows)
            println("✓ Seeded ${pricingRows.size} artist pricing rows")

            // 4. Compute and set startingPrice on each artist
            savedArtists.forEach { artist ->
                val minPrice = artistPricingRepository.findByArtistId(artist.id!!)
                    .minOfOrNull { it.price } ?: 0
                artist.startingPrice = minPrice
            }
            artistRepository.saveAll(savedArtists)
            println("✓ Updated starting prices for ${savedArtists.size} artists")
        }
    }
}
