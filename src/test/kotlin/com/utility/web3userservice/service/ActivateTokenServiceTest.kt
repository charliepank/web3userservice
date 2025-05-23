package com.utility.web3userservice.service

import com.utility.web3userservice.model.ActivateToken
import com.utility.web3userservice.repository.ActivateTokenRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ActivateTokenServiceTest {
    private lateinit var activateTokenService: ActivateTokenService
    private lateinit var activateTokenRepository: ActivateTokenRepository

    @BeforeEach
    fun setup() {
        activateTokenRepository = mockk()
        activateTokenService = ActivateTokenService(activateTokenRepository)
    }

    @Test
    fun `validateToken returns true for valid non-activated token`() {
        // Arrange
        val token = "valid-token"
        val activateToken = ActivateToken(
            token = token,
            userId = "test-user",
            activatedAt = null
        )
        every { activateTokenRepository.findByToken(token) } returns activateToken

        // Act
        val result = activateTokenService.validateToken(token)

        // Assert
        assertTrue(result)
        verify { activateTokenRepository.findByToken(token) }
    }

    @Test
    fun `validateToken returns false for activated token`() {
        // Arrange
        val token = "activated-token"
        val activateToken = ActivateToken(
            token = token,
            userId = "test-user",
            activatedAt = LocalDateTime.now()
        )
        every { activateTokenRepository.findByToken(token) } returns activateToken

        // Act
        val result = activateTokenService.validateToken(token)

        // Assert
        assertFalse(result)
        verify { activateTokenRepository.findByToken(token) }
    }

    @Test
    fun `validateToken returns false for non-existent token`() {
        // Arrange
        val token = "non-existent-token"
        every { activateTokenRepository.findByToken(token) } returns null

        // Act
        val result = activateTokenService.validateToken(token)

        // Assert
        assertFalse(result)
        verify { activateTokenRepository.findByToken(token) }
    }

    @Test
    fun `createToken generates new token and saves it`() {
        // Arrange
        val userId = "test-user"
        val tokenSlot = slot<ActivateToken>()
        every { activateTokenRepository.save(capture(tokenSlot)) } answers { tokenSlot.captured }

        // Act
        val result = activateTokenService.createToken(userId)

        // Assert
        assertNotNull(result)
        assertEquals(userId, tokenSlot.captured.userId)
        assertEquals(result, tokenSlot.captured.token)
        verify { activateTokenRepository.save(any()) }
    }

    @Test
    fun `activateToken returns true and updates token when valid`() {
        // Arrange
        val token = "valid-token"
        val existingToken = ActivateToken(
            token = token,
            userId = "test-user",
            activatedAt = null
        )
        every { activateTokenRepository.findByTokenAndActivatedAtIsNull(token) } returns existingToken
        
        val updatedTokenSlot = slot<ActivateToken>()
        every { activateTokenRepository.save(capture(updatedTokenSlot)) } answers { updatedTokenSlot.captured }

        // Act
        val result = activateTokenService.activateToken(token)

        // Assert
        assertTrue(result)
        assertNotNull(updatedTokenSlot.captured.activatedAt)
        assertNotNull(updatedTokenSlot.captured.updatedAt)
        verify { 
            activateTokenRepository.findByTokenAndActivatedAtIsNull(token)
            activateTokenRepository.save(any())
        }
    }

    @Test
    fun `activateToken returns false for non-existent token`() {
        // Arrange
        val token = "non-existent-token"
        every { activateTokenRepository.findByTokenAndActivatedAtIsNull(token) } returns null

        // Act
        val result = activateTokenService.activateToken(token)

        // Assert
        assertFalse(result)
        verify { activateTokenRepository.findByTokenAndActivatedAtIsNull(token) }
    }
}