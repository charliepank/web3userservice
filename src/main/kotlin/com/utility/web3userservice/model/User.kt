package com.utility.web3userservice.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.index.Indexed
import java.time.LocalDateTime

@Document(collection = "User")
data class User(
    @Id
    val id: String? = null,

    @Indexed(unique = true)
    val email: String,

    val password: String? = null,  // Made nullable with default
    val walletAddress: String? = null,  // Made nullable with default
    val active: Boolean = false,
    val type: LoginType = LoginType.SYSTEM,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)