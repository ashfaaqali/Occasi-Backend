package com.occasi.application.config

import com.occasi.application.model.HennaArtist
import com.occasi.application.model.HennaDesign
import com.occasi.application.repository.HennaArtistRepository
import com.occasi.application.service.HennaArtistService
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class DataSeeder {

    @Bean
    fun initDatabase(repository: HennaArtistRepository, artistService: HennaArtistService) = CommandLineRunner {
        if (repository.count() > 0) return@CommandLineRunner
        
        val artist1 = HennaArtist(
            name = "Ariya",
            email = "ariya@example.com",
            mobileNumber = "9876543210",
            cityName = "Delhi",
            location = "South Delhi",
            rating = 5,
            reviews = 10
        )
        // Add designs
        val designs1 = listOf(
            HennaDesign(imageUrl = "https://placehold.co/200x200", price = 200, complexity = "Simple", category = "Party").apply { artist = artist1 },
            HennaDesign(imageUrl = "https://placehold.co/200x200", price = 500, complexity = "Complex", category = "Bridal").apply { artist = artist1 }
        )
        artist1.designs = designs1
        
        val artist2 = HennaArtist(
            name = "Zara",
            email = "zara@example.com",
            mobileNumber = "9988776655",
            cityName = "Mumbai",
            location = "Bandra",
            rating = 4,
            reviews = 5
        )
        val designs2 = listOf(
             HennaDesign(imageUrl = "https://placehold.co/200x200", price = 300, complexity = "Mid", category = "Party").apply { artist = artist2 }
        )
        artist2.designs = designs2
        
        artistService.registerArtist(artist1)
        artistService.registerArtist(artist2)
        
        println("Seeded database with artists and designs")
    }
}
