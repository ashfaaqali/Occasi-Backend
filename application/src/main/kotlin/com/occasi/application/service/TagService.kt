package com.occasi.application.service

import com.occasi.application.repository.HennaDesignRepository
import com.occasi.application.repository.InvitationCardRepository
import org.springframework.stereotype.Service

@Service
class TagService(
    private val hennaDesignRepository: HennaDesignRepository,
    private val invitationCardRepository: InvitationCardRepository
) {

    fun getDesignTags(): List<String> {
        val tags = hennaDesignRepository.findAll()
            .flatMap { parseTags(it.tags) }
            .distinct()
            .sorted()
        return listOf("ALL") + tags
    }

    fun getInvitationCardTags(): List<String> {
        val tags = invitationCardRepository.findAll()
            .flatMap { parseTags(it.tags) }
            .distinct()
            .sorted()
        return listOf("ALL") + tags
    }

    fun getAllTags(): List<String> {
        val designTags = hennaDesignRepository.findAll().flatMap { parseTags(it.tags) }
        val cardTags = invitationCardRepository.findAll().flatMap { parseTags(it.tags) }
        val tags = (designTags + cardTags).distinct().sorted()
        return listOf("ALL") + tags
    }

    private fun parseTags(raw: String): List<String> =
        raw.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
}
