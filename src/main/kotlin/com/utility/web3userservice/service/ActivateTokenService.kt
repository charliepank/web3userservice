package com.utility.web3userservice.service

import com.utility.web3userservice.repository.ActivateTokenRepository
import com.utility.web3userservice.model.ActivateToken
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
class ActivateTokenService(
    private val activateTokenRepository: ActivateTokenRepository
) {
    fun validateToken(token: String): Boolean {
        val activateToken = activateTokenRepository.findByToken(token)
        return activateToken != null && activateToken.activatedAt == null
    }

    fun createToken(userId: String): String {
        val token = UUID.randomUUID().toString()
        val activateToken = ActivateToken(
            token = token,
            userId = userId
        )
        activateTokenRepository.save(activateToken)
        return token
    }

    fun activateToken(token: String): Boolean {
        val activateToken = activateTokenRepository.findByTokenAndActivatedAtIsNull(token)
            ?: return false

        val updatedToken = activateToken.copy(
            activatedAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        activateTokenRepository.save(updatedToken)
        return true
    }
}