package com.utility.web3userservice.model

import org.junit.jupiter.api.Test
import assertk.assertThat
import assertk.assertions.*

class UserIdentityResponseTest {

    @Test
    fun `UserIdentityResponse creation`() {
        val response = UserIdentityResponse(
            userId = "123",
            email = "test@example.com",
            walletAddress = "0x123456789"
        )

        assertThat(response.userId).isEqualTo("123")
        assertThat(response.email).isEqualTo("test@example.com")
        assertThat(response.walletAddress).isEqualTo("0x123456789")
    }

    @Test
    fun `UserIdentityResponse creation with null wallet address`() {
        val response = UserIdentityResponse(
            userId = "123",
            email = "test@example.com",
            walletAddress = null
        )

        assertThat(response.userId).isEqualTo("123")
        assertThat(response.email).isEqualTo("test@example.com")
        assertThat(response.walletAddress).isNull()
    }
}
