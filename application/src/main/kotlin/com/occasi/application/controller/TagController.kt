package com.occasi.application.controller

import com.occasi.application.constants.BackendRoutes
import com.occasi.application.service.TagService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping(BackendRoutes.Tags.BASE)
class TagController(private val tagService: TagService) {

    @GetMapping
    fun getTags(@RequestParam(required = false) type: String?): List<String> {
        return when (type) {
            "design" -> tagService.getDesignTags()
            "invitation" -> tagService.getInvitationCardTags()
            else -> tagService.getAllTags()
        }
    }
}
