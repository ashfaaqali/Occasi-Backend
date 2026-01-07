package com.occasi.application.config

import com.occasi.application.model.HennaArtist
import com.occasi.application.model.HennaDesign
import com.occasi.application.repository.HennaArtistRepository
import com.occasi.application.service.HennaArtistService
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import kotlin.random.Random

@Configuration
class DataSeeder {

    private val firstNames = listOf(
        "Priya", "Zara", "Aisha", "Fatima", "Sana", "Noor", "Riya", "Amber", "Meera", "Kavya",
        "Ananya", "Diya", "Isha", "Jiya", "Kiara", "Lavanya", "Myra", "Navya", "Oviya", "Pari",
        "Rhea", "Saanvi", "Tara", "Uma", "Vanya", "Wafa", "Yashvi", "Zoya", "Aditi", "Bhavna",
        "Chitra", "Deepa", "Ekta", "Farheen", "Gauri", "Hina", "Ira", "Jasmine", "Komal", "Lata",
        "Madhuri", "Neha", "Pallavi", "Qurat", "Rashmi", "Sakshi", "Tanvi", "Urvashi", "Vidya", "Yamini"
    )

    private val studioSuffixes = listOf(
        "Mehndi Art", "Henna Studio", "Henna Works", "Mehndi Magic", "Henna Creations",
        "Mehndi Designs", "Henna House", "Mehndi Studio", "Henna Art", "Mehndi Palace",
        "Henna Gallery", "Mehndi Corner", "Henna Touch", "Mehndi World", "Henna Dreams"
    )

    private val cities = listOf(
        "Mumbai" to listOf("Bandra West", "Andheri", "Juhu", "Colaba", "Powai"),
        "Delhi" to listOf("Connaught Place", "Hauz Khas", "Saket", "Dwarka", "Rohini"),
        "Bangalore" to listOf("Koramangala", "Indiranagar", "Whitefield", "Jayanagar", "HSR Layout"),
        "Hyderabad" to listOf("Banjara Hills", "Jubilee Hills", "Hitech City", "Gachibowli", "Madhapur"),
        "Pune" to listOf("Koregaon Park", "Viman Nagar", "Kothrud", "Aundh", "Hinjewadi"),
        "Chennai" to listOf("T Nagar", "Anna Nagar", "Adyar", "Velachery", "Nungambakkam"),
        "Kolkata" to listOf("Park Street", "Salt Lake", "New Town", "Ballygunge", "Alipore"),
        "Jaipur" to listOf("MI Road", "C Scheme", "Vaishali Nagar", "Malviya Nagar", "Raja Park"),
        "Ahmedabad" to listOf("Navrangpura", "Satellite", "Prahlad Nagar", "Bodakdev", "SG Highway"),
        "Lucknow" to listOf("Hazratganj", "Gomti Nagar", "Aliganj", "Indira Nagar", "Mahanagar")
    )

    private val categories = listOf("Party", "Bridal", "Festival", "Traditional", "Arabic", "Indo-Arabic")
    private val complexities = listOf("Simple", "Mid", "Complex")

    private fun generateDesigns(artistIndex: Int, count: Int): List<HennaDesign> {
        return (1..count).map { designIndex ->
            val complexity = complexities.random()
            val category = categories.random()
            val price = when (complexity) {
                "Simple" -> Random.nextInt(300, 600)
                "Mid" -> Random.nextInt(600, 1200)
                else -> Random.nextInt(1200, 3500)
            }
            val imageId = (artistIndex * 10) + designIndex + 100
            HennaDesign(
                imageUrl = "https://picsum.photos/id/$imageId/300/300",
                price = price,
                complexity = complexity,
                category = category
            )
        }
    }

    private fun generateArtist(index: Int): HennaArtist {
        val firstName = firstNames[index % firstNames.size]
        val suffix = studioSuffixes[index % studioSuffixes.size]
        val (city, locations) = cities[index % cities.size]
        val location = locations[index % locations.size]

        val artist = HennaArtist(
            name = "$firstName's $suffix",
            email = "${firstName.lowercase()}${index}@example.com",
            mobileNumber = "9${Random.nextInt(100000000, 999999999)}",
            cityName = city,
            location = location,
            rating = (3..5).random().toShort(),
            reviews = Random.nextInt(20, 200),
            coverImage = "https://picsum.photos/id/${index + 10}/400/300"
        )

        val designCount = Random.nextInt(1, 4) // 1-3 designs per artist to reach ~200 total
        val designs = generateDesigns(index, designCount)
        designs.forEach { it.artist = artist }
        artist.designs = designs

        return artist
    }

    @Bean
    fun initDatabase(repository: HennaArtistRepository, artistService: HennaArtistService) = CommandLineRunner {
        if (repository.count() > 0) return@CommandLineRunner

        val artists = (1..100).map { generateArtist(it) }

        artists.forEach { artistService.registerArtist(it) }
        println("✓ Seeded ${artists.size} artists with ${artists.sumOf { it.designs.size }} designs")
    }
}
