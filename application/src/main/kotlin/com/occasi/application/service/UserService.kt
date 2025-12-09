package com.occasi.application.service

import com.occasi.application.model.User
import com.occasi.application.repository.UserRepository
import org.springframework.stereotype.Service

@Service
class UserService(private val repository: UserRepository) {
    fun registerUser(user: User): User = repository.save(user)
    fun getUser(id: Long): User = repository.findById(id).orElse(null)
    fun getAllUsers(): List<User> = repository.findAll()
}
