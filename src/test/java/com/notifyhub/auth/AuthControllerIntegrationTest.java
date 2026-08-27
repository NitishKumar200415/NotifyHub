package com.notifyhub.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notifyhub.IntegrationTest;
import com.notifyhub.auth.dto.LoginRequest;
import com.notifyhub.auth.dto.RegisterRequest;
import com.notifyhub.user.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AuthControllerIntegrationTest extends IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AppUserRepository appUserRepository;


    @Test
    void shouldRegisterUserAndReturnJwtToken() throws Exception {

        RegisterRequest request = new RegisterRequest(
                "api-register@test.com",
                "password123"
        );

        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(request)
                                )
                )
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath("$.accessToken").isNotEmpty()
                )
                .andExpect(
                        jsonPath("$.tokenType").value("Bearer")
                )
                .andExpect(
                        jsonPath("$.email")
                                .value("api-register@test.com")
                )
                .andExpect(
                        jsonPath("$.role").value("CLIENT")
                )
                .andExpect(
                        jsonPath("$.expiresInMs").isNumber()
                );

        assertThat(
                appUserRepository
                        .findByEmail("api-register@test.com")
        ).isPresent();
    }


    @Test
    void shouldLoginRegisteredUserAndReturnJwtToken()
            throws Exception {

        RegisterRequest registerRequest =
                new RegisterRequest(
                        "api-login@test.com",
                        "password123"
                );

        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                registerRequest
                                        )
                                )
                )
                .andExpect(status().isCreated());

        LoginRequest loginRequest =
                new LoginRequest(
                        "api-login@test.com",
                        "password123"
                );

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                loginRequest
                                        )
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.accessToken").isNotEmpty()
                )
                .andExpect(
                        jsonPath("$.tokenType").value("Bearer")
                )
                .andExpect(
                        jsonPath("$.email")
                                .value("api-login@test.com")
                )
                .andExpect(
                        jsonPath("$.role").value("CLIENT")
                );
    }


    @Test
    void shouldRejectDuplicateEmailRegistration()
            throws Exception {

        RegisterRequest request =
                new RegisterRequest(
                        "duplicate@test.com",
                        "password123"
                );

        String requestBody =
                objectMapper.writeValueAsString(request);

        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isCreated());

        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isConflict());
    }


    @Test
    void shouldRejectInvalidLoginCredentials()
            throws Exception {

        RegisterRequest registerRequest =
                new RegisterRequest(
                        "wrong-password@test.com",
                        "password123"
                );

        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                registerRequest
                                        )
                                )
                )
                .andExpect(status().isCreated());

        LoginRequest loginRequest =
                new LoginRequest(
                        "wrong-password@test.com",
                        "wrongpassword"
                );

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                loginRequest
                                        )
                                )
                )
                .andExpect(status().isUnauthorized());
    }
}