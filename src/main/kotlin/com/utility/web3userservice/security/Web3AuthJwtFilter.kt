package com.utility.web3userservice.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter
import com.utility.web3userservice.repository.UserRepository
import com.utility.web3userservice.service.UserService
import org.springframework.web.server.ResponseStatusException
import org.springframework.http.HttpStatus
import org.springframework.security.core.authority.SimpleGrantedAuthority

class Web3AuthJwtFilter : OncePerRequestFilter() {

    @Autowired
    private lateinit var userService: UserService  // For internal validation

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        logger.debug("Web3AuthJwtFilter processing request to: ${request.requestURI}")

        val cookies = request.cookies
        logger.debug("Cookies present: ${cookies?.map { "${it.name}=${it.value}" }}")

        val logtoken = cookies?.firstOrNull { it.name == "AUTH-TOKEN" }?.value
        logger.debug("Auth token found: ${logtoken != null}")

        // Skip filter for login endpoint since it handles its own auth
        if (request.requestURI.endsWith("/api/user/login")) {
            filterChain.doFilter(request, response)
            return
        }

        val token = request.cookies?.firstOrNull { it.name == "AUTH-TOKEN" }?.value

        if (token != null) {
            try {
                // Internal validation only - no external Web3Auth call
                val userId = userService.validateInternalToken(token)

                val authentication = UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    listOf(SimpleGrantedAuthority("ROLE_USER"))
                )

                SecurityContextHolder.getContext().authentication = authentication
            } catch (e: Exception) {
                logger.error("Could not authenticate user", e)
                // Optionally clear security context
                SecurityContextHolder.clearContext()
            }
        }

        filterChain.doFilter(request, response)
    }
}