package com.occasi.application.controller

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
}
