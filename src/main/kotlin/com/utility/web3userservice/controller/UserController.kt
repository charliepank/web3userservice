package com.utility.web3userservice.controller

import com.utility.web3userservice.model.UserIdentityResponse
import com.utility.web3userservice.service.UserService
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.web.bind.annotation.*
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.http.ResponseCookie
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import java.time.Duration
import java.time.Instant
import java.util.Date
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag

@RestController
@RequestMapping("/api/user")
@Tag(name = "User Management", description = "APIs for user authentication and management")
class UserController(
    private val userService: UserService
) {
    private val logger = LoggerFactory.getLogger(UserController::class.java)
    @Value("\${cookie.domain}")
    private lateinit var cookieDomain: String
    @Value("\${jwt.secret}")
    private lateinit var jwtSecret: String

    @Operation(summary = "Login with Web3Auth", description = "Authenticate user with Web3Auth token and wallet address")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successfully authenticated"),
        ApiResponse(responseCode = "400", description = "Invalid request parameters"),
        ApiResponse(responseCode = "401", description = "Authentication failed")
    ])
    @PostMapping("/login")
    fun login(
        @Parameter(description = "Web3Auth Bearer token") @RequestHeader("Authorization") authHeader: String,
        @Parameter(description = "Flag to clear existing cookies") @RequestHeader("X-Clear-Cookies", required = false) clearCookies: String?,
        @Parameter(description = "Request body containing wallet address") @RequestBody body: Map<String, String>
    ): ResponseEntity<UserIdentityResponse> {
        val web3AuthToken = authHeader.removePrefix("Bearer ").trim()
        val walletAddress = body["address"]
            ?: return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(null)

        val identity = userService.getIdentityFromToken(web3AuthToken, walletAddress)

        // Create our internal JWT
        val token = Jwts.builder()
            .setSubject(identity.userId)
            .setIssuedAt(Date.from(Instant.now()))
            .setExpiration(Date.from(Instant.now().plus(Duration.ofHours(24))))
            .signWith(Keys.hmacShaKeyFor(jwtSecret.toByteArray()))
            .compact()

        val authCookie = ResponseCookie.from("AUTH-TOKEN", token)
            .path("/")
            .secure(true)
            .domain(cookieDomain)
            .maxAge(Duration.ofHours(24))
            .sameSite("None")
            .build()

        val responseBuilder = ResponseEntity.ok()
            .header("Set-Cookie", authCookie.toString())

        // If X-Clear-Cookies is true, clear all cookies
        if (clearCookies == "true") {
            val clearedCookie = ResponseCookie.from("AUTH-TOKEN", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .build()
            responseBuilder.header("Set-Cookie", clearedCookie.toString())
        }

        return responseBuilder.body(identity)
    }

    @Operation(summary = "Get user identity", description = "Retrieve current user's identity information")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successfully retrieved user identity"),
        ApiResponse(responseCode = "401", description = "User not authenticated")
    ])
    @GetMapping("/identity")
    fun getUserIdentity(): ResponseEntity<UserIdentityResponse> {
        val userId = SecurityContextHolder.getContext().authentication.principal as String
        return ResponseEntity.ok(userService.getUserById(userId))
    }

    @Operation(summary = "Logout user", description = "Clear user's authentication cookies")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successfully logged out")
    ])
    @PostMapping("/logout")
    fun logout(): ResponseEntity<Unit> {
        val clearedCookie = ResponseCookie.from("AUTH-TOKEN", "")
            .httpOnly(true)
            .secure(true)
            .path("/")
            .maxAge(0)
            .build()

        return ResponseEntity.ok()
            .header("Set-Cookie", clearedCookie.toString())
            .build()
    }
}