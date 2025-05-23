package com.utility.web3userservice.model

import org.junit.jupiter.api.Test
import assertk.assertThat
import assertk.assertions.*
import java.time.LocalDateTime

class ActivateTokenTest {

    @Test
    fun `ActivateToken creation with all fields`() {
        val now = LocalDateTime.now()
        val token = ActivateToken(
            id = "123",
            token = "token123",
            activatedAt = now,
            createdAt = now,
            updatedAt = now,
            userId = "user123"
        )

        assertThat(token.id).isEqualTo("123")
        assertThat(token.token).isEqualTo("token123")
        assertThat(token.activatedAt).isEqualTo(now)
        assertThat(token.createdAt).isEqualTo(now)
        assertThat(token.updatedAt).isEqualTo(now)
        assertThat(token.userId).isEqualTo("user123")
    }

    @Test
    fun `ActivateToken creation with minimal fields uses defaults`() {
        val token = ActivateToken(
            token = "token123",
            userId = "user123"
        )

        assertThat(token.id).isNull()
        assertThat(token.token).isEqualTo("token123")
        assertThat(token.activatedAt).isNull()
        assertThat(token.createdAt).isNotNull()
        assertThat(token.updatedAt).isNotNull()
        assertThat(token.userId).isEqualTo("user123")
    }
}
