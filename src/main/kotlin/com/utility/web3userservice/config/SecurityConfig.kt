// SecurityConfig.kt
package com.utility.web3userservice.config

import com.utility.web3userservice.security.Web3AuthJwtFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfigurationSource

@Configuration
@EnableWebSecurity
class SecurityConfig(
) {
    @Bean
    fun web3AuthFilter() = Web3AuthJwtFilter()

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .securityMatchers { it.requestMatchers("/api/**", "/swagger-ui/**", "/v3/api-docs/**") }
            .csrf { it.disable() }
            .authorizeHttpRequests {
                it.requestMatchers("/api/user/login").permitAll()
                    .requestMatchers("/api/user/identity").permitAll()
                    .requestMatchers("/api/token/activate/**").permitAll()
                    .requestMatchers("/api/token/validate/**").permitAll()
                    .requestMatchers("/swagger-ui/**").permitAll()
                    .requestMatchers("/v3/api-docs/**").permitAll()
                    .anyRequest().authenticated()
            }
            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .addFilterBefore(web3AuthFilter(), UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }
}