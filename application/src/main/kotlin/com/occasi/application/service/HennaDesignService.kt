package com.occasi.application.service

import com.occasi.application.model.HennaDesign
import com.occasi.application.repository.HennaDesignRepository
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class HennaDesignService(private val repository: HennaDesignRepository) {

    @Cacheable("hennaDesigns")
    fun getAllDesigns(): List<HennaDesign> = repository.findAll()

    fun getDesignsByComplexity(complexity: String): List<HennaDesign> = repository.findByComplexity(complexity)

    @Cacheable("designDetail", key = "#id")
    fun getDesignById(id: Long): HennaDesign? = repository.findById(id).orElse(null)

    @CacheEvict(value = ["hennaDesigns", "designDetail"], allEntries = true)
    fun saveDesign(design: HennaDesign): HennaDesign {
        return repository.save(design)
    }

    @CacheEvict(value = ["hennaDesigns", "designDetail"], allEntries = true)
    fun deleteDesign(id: Long) = repository.deleteById(id)
}
