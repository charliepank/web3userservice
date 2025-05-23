package com.utility.web3userservice.model

data class UserIdentityResponse(
    val userId: String,
    val email: String,
    val walletAddress: String?
)