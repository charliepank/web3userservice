package com.utility.web3userservice

import io.github.cdimascio.dotenv.dotenv
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class UserServiceApplication {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            // Load .env file if it exists, otherwise continue without it
            dotenv {
                ignoreIfMissing = true
                directory = "./"  // Look in the current directory
            }
            
            runApplication<UserServiceApplication>(*args)
        }
    }
}