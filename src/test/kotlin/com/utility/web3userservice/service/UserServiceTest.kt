package com.utility.web3userservice.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.utility.web3userservice.model.LoginType
import com.utility.web3userservice.model.User
import com.utility.web3userservice.model.UserIdentityResponse
import com.utility.web3userservice.repository.ActivateTokenRepository
import com.utility.web3userservice.repository.UserRepository
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jws
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.JwtParser
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import io.jsonwebtoken.security.SignatureException
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.web.client.RestTemplate
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.time.LocalDateTime
import java.util.Base64
import java.util.Date
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class UserServiceTest {
    private val logger = LoggerFactory.getLogger(UserServiceTest::class.java)
    private lateinit var userService: UserService
    private val userRepository: UserRepository = mockk()
    private val activateTokenRepository: ActivateTokenRepository = mockk()
    private val testSecret = "your-test-secret-key-that-is-long-enough-for-testing"

    @BeforeEach
    fun setup() {
        userService = spyk(UserService(userRepository, activateTokenRepository))
        ReflectionTestUtils.setField(userService, "jwtSecret", testSecret)
    }

    @Test
    fun `getUserById returns user when found`() {
        // Arrange
        val userId = "test-user-id"
        val expectedUser = User(
            id = userId,
            email = "test@example.com",
            walletAddress = "0x123",
            active = true,
            type = LoginType.WEB3AUTH
        )
        every { userRepository.findById(userId) } returns Optional.of(expectedUser)

        // Act
        val result = userService.getUserById(userId)

        // Assert
        assertNotNull(result)
        assertEquals(expectedUser.id, result.userId)
        assertEquals(expectedUser.email, result.email)
        assertEquals(expectedUser.walletAddress, result.walletAddress)
        verify(exactly = 1) { userRepository.findById(userId) }
    }

    @Test
    fun `getUserById throws exception when user not found`() {
        // Arrange
        val userId = "non-existent-id"
        every { userRepository.findById(userId) } returns Optional.empty()

        // Act & Assert
        val exception = assertThrows<ResponseStatusException> {
            userService.getUserById(userId)
        }
        assertEquals(HttpStatus.NOT_FOUND, exception.statusCode)
        verify(exactly = 1) { userRepository.findById(userId) }
    }

    @Test
    fun `validateInternalToken returns userId for valid token`() {
        // Arrange
        val userId = "test-user-id"
        val token = Jwts.builder()
            .setSubject(userId)
            .setIssuedAt(Date())
            .setExpiration(Date.from(Instant.now().plusSeconds(3600))) // 1 hour from now
            .signWith(Keys.hmacShaKeyFor(testSecret.toByteArray()))
            .compact()

        // Act
        val result = userService.validateInternalToken(token)

        // Assert
        assertEquals(userId, result)
    }

    @Test
    fun `validateInternalToken throws exception for expired token`() {
        // Arrange
        val userId = "test-user-id"
        val token = Jwts.builder()
            .setSubject(userId)
            .setIssuedAt(Date.from(Instant.now().minusSeconds(7200))) // 2 hours ago
            .setExpiration(Date.from(Instant.now().minusSeconds(3600))) // 1 hour ago
            .signWith(Keys.hmacShaKeyFor(testSecret.toByteArray()))
            .compact()

        // Act & Assert
        val exception = assertThrows<InvalidTokenException> {
            userService.validateInternalToken(token)
        }
        assert(exception.message?.contains("expired", ignoreCase = true) == true)
    }

    @Test
    fun `validateInternalToken throws exception for invalid token`() {
        // Arrange
        val invalidToken = "invalid.token.here"

        // Act & Assert
        assertThrows<InvalidTokenException> {
            userService.validateInternalToken(invalidToken)
        }
    }

    @Test
    fun `getIdentityFromToken creates new user when user doesn't exist`() {
        // Arrange
        val token = "valid.web3auth.token"
        val walletAddress = "0x123456789"
        val email = "test@example.com"
        val tokenPayload = UserService.Web3AuthTokenPayload(
            email = email,
            name = "Test User",
            wallets = listOf(UserService.Web3AuthTokenPayload.Wallet(
                public_key = "test-key",
                type = "test",
                curve = "test"
            )),
            verifier = "test-verifier",
            verifierId = "test-verifier-id",
            iss = "test-issuer",
            aud = "test-audience",
            exp = Instant.now().plusSeconds(3600).epochSecond,
            iat = Instant.now().epochSecond,
            nonce = null,
            profileImage = null,
            aggregateVerifier = null
        )
        
        val newUser = User(
            id = "new-user-id",
            email = email,
            walletAddress = walletAddress,
            active = true,
            type = LoginType.WEB3AUTH
        )

        // Mock the repository calls
        every { userRepository.findAll() } returns listOf()
        every { userRepository.findByEmail(email) } returns null
        every { userRepository.save(any()) } returns newUser

        // Mock the validateWeb3AuthToken method
        every { userService["validateWeb3AuthToken"](token) } returns tokenPayload

        // Act
        val result = userService.getIdentityFromToken(token, walletAddress)

        // Assert
        assertNotNull(result)
        assertEquals(newUser.id, result.userId)
        assertEquals(email, result.email)
        assertEquals(walletAddress, result.walletAddress)
        
        // Verify repository calls
        verify { 
            userRepository.findAll()
            userRepository.findByEmail(email)
            userRepository.save(match { 
                it.email == email && 
                it.walletAddress == walletAddress && 
                it.type == LoginType.WEB3AUTH 
            })
        }
    }
} 