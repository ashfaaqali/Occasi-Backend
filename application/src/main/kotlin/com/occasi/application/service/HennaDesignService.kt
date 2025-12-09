package com.occasi.application.service

import com.occasi.application.model.HennaDesign
import com.occasi.application.repository.HennaDesignRepository
import org.springframework.stereotype.Service

@Service
class HennaDesignService(private val repository: HennaDesignRepository) {
    fun getAllDesigns(): List<HennaDesign> = repository.findAll()
    
    fun getDesignsByPriceRange(min: Int, max: Int): List<HennaDesign> = repository.findByPriceBetween(min, max)
    
    fun getDesignsByComplexity(complexity: String): List<HennaDesign> = repository.findByComplexity(complexity)
}
