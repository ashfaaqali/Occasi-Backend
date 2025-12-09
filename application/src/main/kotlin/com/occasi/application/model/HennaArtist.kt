package com.occasi.application.model

import jakarta.persistence.*

@Entity
data class HennaArtist(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    
    var name: String,
    var email: String,
    var mobileNumber: String,
    var cityName: String,
    var location: String,
    var rating: Short = 0,
    var reviews: Int = 0,
    var coverImage: String? = null,
    var startingPrice: Int = 0
) {
    @OneToMany(mappedBy = "artist", cascade = [CascadeType.ALL])
    @com.fasterxml.jackson.annotation.JsonManagedReference
    var designs: List<HennaDesign> = ArrayList()
}
