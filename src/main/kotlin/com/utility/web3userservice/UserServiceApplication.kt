package com.utility.web3userservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import io.github.cdimascio.dotenv.dotenv

@SpringBootApplication
class UserServiceApplication {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            // Load environment variables before Spring Boot starts
            dotenv {
                systemProperties = true
            }
            runApplication<com.utility.web3userservice.UserServiceApplication>(*args)
        }
    }
}