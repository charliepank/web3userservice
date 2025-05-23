package com.utility.web3userservice.repository

import com.utility.web3userservice.model.ActivateToken
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface ActivateTokenRepository : MongoRepository<ActivateToken, String> {
    fun findByToken(token: String): ActivateToken?
    fun findByUserId(userId: String): List<ActivateToken>
    fun findByTokenAndActivatedAtIsNull(token: String): ActivateToken?
}