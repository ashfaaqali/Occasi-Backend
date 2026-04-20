package com.occasi.application.service

import com.occasi.application.model.User
import com.occasi.application.repository.UserRepository
import org.springframework.stereotype.Service

@Service
class UserService(private val repository: UserRepository) {
    fun registerUser(user: User): User = repository.save(user)
    fun getUser(id: Long): User = repository.findById(id).orElse(null)
    fun getAllUsers(): List<User> = repository.findAll()

    fun updateProfile(id: Long, name: String?, email: String?, phone: String?): User {
        val user = repository.findById(id).orElseThrow { RuntimeException("User not found") }
        if (!name.isNullOrBlank()) user.name = name
        if (!email.isNullOrBlank()) user.email = email
        if (!phone.isNullOrBlank() && user.mobileNumber.isBlank()) user.mobileNumber = phone
        return repository.save(user)
    }
}
