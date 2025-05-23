package com.utility.web3userservice.model

import org.junit.jupiter.api.Test
import assertk.assertThat
import assertk.assertions.*
import java.time.LocalDateTime

class UserTest {

    @Test
    fun `User creation with all fields`() {
        val now = LocalDateTime.now()
        val user = User(
            id = "123",
            email = "test@example.com",
            password = "hashedPassword",
            walletAddress = "0x123456789",
            active = true,
            type = LoginType.WEB3AUTH,
            createdAt = now,
            updatedAt = now
        )

        assertThat(user.id).isEqualTo("123")
        assertThat(user.email).isEqualTo("test@example.com")
        assertThat(user.password).isEqualTo("hashedPassword")
        assertThat(user.walletAddress).isEqualTo("0x123456789")
        assertThat(user.active).isTrue()
        assertThat(user.type).isEqualTo(LoginType.WEB3AUTH)
        assertThat(user.createdAt).isEqualTo(now)
        assertThat(user.updatedAt).isEqualTo(now)
    }

    @Test
    fun `User creation with minimal fields uses defaults`() {
        val user = User(
            email = "test@example.com",
            password = null,
            walletAddress = null
        )

        assertThat(user.id).isNull()
        assertThat(user.email).isEqualTo("test@example.com")
        assertThat(user.password).isNull()
        assertThat(user.walletAddress).isNull()
        assertThat(user.active).isFalse()
        assertThat(user.type).isEqualTo(LoginType.SYSTEM)
        assertThat(user.createdAt).isNotNull()
        assertThat(user.updatedAt).isNotNull()
    }
}
