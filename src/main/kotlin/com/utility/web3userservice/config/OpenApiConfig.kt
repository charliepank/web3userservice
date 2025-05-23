package com.utility.web3userservice.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.Contact
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {
    @Bean
    fun openAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("Web3 User Service API")
                    .description("API documentation for Web3 User Service")
                    .version("1.0")
                    .contact(
                        Contact()
                            .name("Your Team Name")
                            .email("your.email@example.com")
                    )
            )
    }
} 