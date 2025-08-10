package io.availe.demo.domain

import java.util.UUID

data class UserModel(
    val id: UUID,
    val firstName: String,
    val lastName: String,
    val email: String,
) {
    init {
        require(firstName.isNotBlank()) { "firstName cannot be blank" }
        require(lastName.isNotBlank()) { "lastName cannot be blank" }
    }
}