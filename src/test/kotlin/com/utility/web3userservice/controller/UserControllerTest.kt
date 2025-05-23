package com.utility.web3userservice.controller

import com.ninjasquad.springmockk.MockkBean
import com.utility.web3userservice.config.TestSecurityConfig
import com.utility.web3userservice.model.UserIdentityResponse
import com.utility.web3userservice.service.UserService
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import org.springframework.test.util.ReflectionTestUtils

@WebMvcTest(controllers = [UserController::class])
@Import(TestSecurityConfig::class)
class UserControllerTest {
    private val logger = LoggerFactory.getLogger(UserControllerTest::class.java)

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var userService: UserService

    @Value("\${jwt.secret}")
    private lateinit var jwtSecret: String

    @BeforeEach
    fun setup() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(webApplicationContext)
            .apply<DefaultMockMvcBuilder>(SecurityMockMvcConfigurers.springSecurity())
            .build()
        
        // Set test values for properties
        ReflectionTestUtils.setField(webApplicationContext.getBean(UserController::class.java), "cookieDomain", "localhost")
    }

    @Test
    fun `login should return 400 when address is missing`() {
        mockMvc.perform(
            post("/api/user/login")
                .header("Authorization", "Bearer mock-web3-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `login should return 200 and set cookie when successful`() {
        val walletAddress = "0x123"
        val userId = "user123"
        val mockIdentity = UserIdentityResponse(
            userId = userId,
            email = "test@example.com",
            walletAddress = walletAddress
        )

        // Mock the token validation
        every { 
            userService.getIdentityFromToken(any(), eq(walletAddress)) 
        } returns mockIdentity

        mockMvc.perform(
            post("/api/user/login")
                .header("Authorization", "Bearer mock-web3-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"address\":\"$walletAddress\"}")
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isOk)
            .andExpect(cookie().exists("AUTH-TOKEN"))
            .andExpect(jsonPath("$.userId").value(userId))
            .andExpect(jsonPath("$.walletAddress").value(walletAddress))
            .andExpect(jsonPath("$.email").value("test@example.com"))

        verify { 
            userService.getIdentityFromToken(any(), eq(walletAddress))
        }
    }

    @Test
    fun `getUserIdentity should return user details for authenticated user`() {
        val userId = "test-user"
        val mockIdentity = UserIdentityResponse(
            userId = userId,
            email = "test@example.com",
            walletAddress = "0x123"
        )

        every { userService.getUserById(userId) } returns mockIdentity

        val auth = TestingAuthenticationToken(userId, null, listOf(SimpleGrantedAuthority("ROLE_USER")))
        auth.isAuthenticated = true

        mockMvc.perform(
            get("/api/user/identity")
                .with(authentication(auth))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.userId").value(userId))
            .andExpect(jsonPath("$.email").value("test@example.com"))
            .andExpect(jsonPath("$.walletAddress").value("0x123"))

        verify { userService.getUserById(userId) }
    }

    @Test
    fun `logout should clear auth cookie`() {
        val auth = TestingAuthenticationToken("test-user", null, listOf(SimpleGrantedAuthority("ROLE_USER")))
        auth.isAuthenticated = true

        mockMvc.perform(
            post("/api/user/logout")
                .with(authentication(auth))
        )
            .andExpect(status().isOk)
            .andExpect(cookie().maxAge("AUTH-TOKEN", 0))
    }
}
