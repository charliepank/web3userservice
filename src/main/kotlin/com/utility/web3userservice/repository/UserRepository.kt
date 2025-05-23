package com.utility.web3userservice.repository

import com.utility.web3userservice.model.User
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : MongoRepository<User, String> {
    fun findByEmail(email: String): User?
    fun findByWalletAddress(walletAddress: String): User?
    fun existsByEmail(email: String): Boolean
}