package com.utility.web3userservice.service

import com.utility.web3userservice.model.UserIdentityResponse
import com.utility.web3userservice.repository.UserRepository
import com.utility.web3userservice.repository.ActivateTokenRepository
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import org.springframework.http.HttpStatus
import java.security.KeyFactory
import java.security.Key
import java.security.spec.ECPublicKeySpec
import java.util.Base64
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.convertValue
import com.utility.web3userservice.model.LoginType
import com.utility.web3userservice.model.User
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.client.RestTemplate
import java.math.BigInteger
import java.time.Instant
import java.util.Date

@Service
class UserService(
    private val userRepository: UserRepository,
    private val activateTokenRepository: ActivateTokenRepository
) {
    private val logger = LoggerFactory.getLogger(UserService::class.java)
    private val restTemplate = RestTemplate()
    @Value("\${jwt.secret}")
    private lateinit var jwtSecret: String

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Web3AuthTokenPayload(
        val email: String,
        val name: String?,
        val wallets: List<Wallet>,
        val verifier: String,
        val verifierId: String,
        val iss: String,
        val aud: String,
        val exp: Long,
        val iat: Long,
        val nonce: String?,
        val profileImage: String?,
        val aggregateVerifier: String?
    ) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        data class Wallet(
            val public_key: String,
            val type: String,
            val curve: String
        )
    }

    data class JwkResponse(
        val keys: List<JwkKey>
    )

    data class JwkKey(
        val kid: String?,
        val kty: String?,
        val alg: String?,
        val use: String?,
        val crv: String? = null,
        val x: String? = null,
        val y: String? = null,
        val n: String? = null,
        val e: String? = null,
        val x5c: List<String>? = null
    )

    fun getIdentityFromToken(token: String, walletAddress: String): UserIdentityResponse {
        logger.info("Starting token validation")
        val tokenPayload = validateWeb3AuthToken(token)
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token")

        logger.info("Token validated, looking up user with email: ${tokenPayload.email}")
        try {
            val allUsers = userRepository.findAll()
            logger.info("Total users in DB: ${allUsers.count()}")
            allUsers.forEach {
                logger.info("Found user: ${it.email}")
            }

            val user = userRepository.findByEmail(tokenPayload.email) ?: createNewUser(tokenPayload, walletAddress)
            if (user == null) {
                logger.info("User not found for email: ${tokenPayload.email}")
            }

            return UserIdentityResponse(
                userId = user.id ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "User ID is null"),
                email = user.email,
                walletAddress = user.walletAddress ?: walletAddress
            )
        } catch (e: Exception) {
            logger.error("Error during user lookup", e)
            throw e
        }
    }

    private fun createNewUser(tokenPayload: Web3AuthTokenPayload, walletAddress: String): User {
        val user = User(
            email = tokenPayload.email,
            walletAddress = walletAddress, //tokenPayload.wallets.firstOrNull()?.public_key,
            active = true,
            type = LoginType.WEB3AUTH
        )
        return userRepository.save(user)
    }

    private fun validateWeb3AuthToken(token: String): Web3AuthTokenPayload? {
        return try {
            logger.info("Validating token: ${token.take(20)}...")

            // Get the public key from Web3Auth
            val jwks = restTemplate.getForObject("https://api-auth.web3auth.io/jwks", JwkResponse::class.java)
                ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Could not fetch JWKS")

            // Parse the JWT header to get the key ID
            val headerJson = String(Base64.getUrlDecoder().decode(token.split(".")[0]))
            val header = jacksonObjectMapper().readValue<Map<String, String>>(headerJson)
            val kid = header["kid"] ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "No key ID in token")

            // Find the matching key
            val matchingKey = jwks.keys.find { it.kid == kid }
                ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "No matching key found")

            // Convert JWK to public key
            val publicKey = convertJwkToPublicKey(matchingKey)

            // Verify token signature and claims
            val claims = Jwts.parserBuilder()
                .setSigningKey(publicKey)
                .build()
                .parseClaimsJws(token)
                .body

            // Parse the payload
            val mapper = jacksonObjectMapper()
            val result = mapper.convertValue<Web3AuthTokenPayload>(claims)

            // Validate expiration
            val now = System.currentTimeMillis() / 1000
            if (result.exp < now) {
                logger.error("Token has expired")
                return null
            }

            logger.info("Successfully validated token for email: ${result.email}")
            result

        } catch (e: Exception) {
            logger.error("Token validation failed", e)
            null
        }
    }

    private fun convertJwkToPublicKey(jwk: JwkKey): Key {
        if (jwk.kty != "EC" || jwk.x == null || jwk.y == null) {
            throw IllegalArgumentException("Invalid EC key parameters")
        }

        val x = Base64.getUrlDecoder().decode(jwk.x)
        val y = Base64.getUrlDecoder().decode(jwk.y)

        val parameterSpec = java.security.spec.ECParameterSpec(
            java.security.spec.EllipticCurve(
                java.security.spec.ECFieldFp(
                    BigInteger("FFFFFFFF00000001000000000000000000000000FFFFFFFFFFFFFFFFFFFFFFFF", 16)
                ),
                BigInteger("FFFFFFFF00000001000000000000000000000000FFFFFFFFFFFFFFFFFFFFFFFC", 16),
                BigInteger("5AC635D8AA3A93E7B3EBBD55769886BC651D06B0CC53B0F63BCE3C3E27D2604B", 16)
            ),
            java.security.spec.ECPoint(
                BigInteger("6B17D1F2E12C4247F8BCE6E563A440F277037D812DEB33A0F4A13945D898C296", 16),
                BigInteger("4FE342E2FE1A7F9B8EE7EB4A7C0F9E162BCE33576B315ECECBB6406837BF51F5", 16)
            ),
            BigInteger("FFFFFFFF00000000FFFFFFFFFFFFFFFFBCE6FAADA7179E84F3B9CAC2FC632551", 16),
            1
        )

        val spec = ECPublicKeySpec(
            java.security.spec.ECPoint(
                BigInteger(1, x),
                BigInteger(1, y)
            ),
            parameterSpec
        )

        val keyFactory = KeyFactory.getInstance("EC")
        return keyFactory.generatePublic(spec)
    }

    fun getUserById(userId: String): UserIdentityResponse {
        val user = userRepository.findById(userId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
        }

        return UserIdentityResponse(
            userId = user.id!!,
            email = user.email,
            walletAddress = user.walletAddress
        )
    }
    fun validateInternalToken(token: String): String {
        try {
            val claims: Claims = Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(jwtSecret.toByteArray()))
                .build()
                .parseClaimsJws(token)
                .body

            // Check if token has expired
            val expiration = claims.expiration
            if (expiration.before(Date.from(Instant.now()))) {
                throw TokenExpiredException("Token has expired")
            }

            // Get user ID from claims
            return claims.subject ?: throw InvalidTokenException("Token missing subject claim")

        } catch (e: Exception) {
            when (e) {
                is TokenExpiredException -> throw e
                else -> throw InvalidTokenException("Invalid token: ${e.message}")
            }
        }
    }
}

class TokenExpiredException(message: String) : RuntimeException(message)
class InvalidTokenException(message: String) : RuntimeException(message)