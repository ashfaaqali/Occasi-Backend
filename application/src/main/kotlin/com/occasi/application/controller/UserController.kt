package com.occasi.application.controller

import com.occasi.application.dto.UserDto as UserDtoResponse
import com.occasi.application.model.User
import com.occasi.application.service.UserService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/users")
class UserController(private val service: UserService) {
    @PostMapping
    fun registerUser(@RequestBody user: User) = service.registerUser(user)

    @GetMapping
    fun getAllUsers() = service.getAllUsers()
    
    @GetMapping("/{id}")
    fun getUser(@PathVariable id: Long) = service.getUser(id)

    @PatchMapping("/{id}/profile")
    fun updateProfile(
        @PathVariable id: Long,
        @RequestBody request: UpdateProfileRequest
    ): UserDtoResponse {
        val user = service.updateProfile(id, request.name, request.email, request.mobileNumber)
        return UserDtoResponse(
            id = user.id!!,
            name = user.name,
            email = user.email,
            mobileNumber = user.mobileNumber,
            role = user.role.name
        )
    }
}

data class UpdateProfileRequest(
    val name: String? = null,
    val email: String? = null,
    val mobileNumber: String? = null
)
