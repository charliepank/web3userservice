package com.utility.web3userservice.controller

import com.utility.web3userservice.service.ActivateTokenService
import org.springframework.web.bind.annotation.*
import org.springframework.http.ResponseEntity
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag

@RestController
@RequestMapping("/api/token")
@Tag(name = "Token Management", description = "APIs for managing activation tokens")
class ActivateTokenController(
    private val activateTokenService: ActivateTokenService
) {
    @Operation(summary = "Create activation token", description = "Generate a new activation token for a user")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Token successfully created"),
        ApiResponse(responseCode = "401", description = "Unauthorized request")
    ])
    @PostMapping("/create/{userId}")
    fun createToken(
        @Parameter(description = "ID of the user to create token for") 
        @PathVariable userId: String
    ): ResponseEntity<String> {
        val token = activateTokenService.createToken(userId)
        return ResponseEntity.ok(token)
    }

    @Operation(summary = "Activate token", description = "Activate an existing token")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Token activation status returned"),
        ApiResponse(responseCode = "404", description = "Token not found")
    ])
    @PostMapping("/activate/{token}")
    fun activateToken(
        @Parameter(description = "Token to activate") 
        @PathVariable token: String
    ): ResponseEntity<Boolean> {
        val activated = activateTokenService.activateToken(token)
        return ResponseEntity.ok(activated)
    }

    @Operation(summary = "Validate token", description = "Check if a token is valid and not yet activated")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Token validation status returned")
    ])
    @GetMapping("/validate/{token}")
    fun validateToken(
        @Parameter(description = "Token to validate") 
        @PathVariable token: String
    ): ResponseEntity<Boolean> {
        val isValid = activateTokenService.validateToken(token)
        return ResponseEntity.ok(isValid)
    }
}