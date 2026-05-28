package com.ulises.udiplomacy.infrastructure.web.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ulises.udiplomacy.application.port.input.AuthenticateUserUseCase;
import com.ulises.udiplomacy.application.port.input.RegisterUserUseCase;
import com.ulises.udiplomacy.infrastructure.web.dto.request.LoginRequest;
import com.ulises.udiplomacy.infrastructure.web.dto.request.RegisterRequest;
import com.ulises.udiplomacy.infrastructure.web.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerTest {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    AuthControllerTest() {
        RegisterUserUseCase registerUserUseCase = mock(RegisterUserUseCase.class);
        AuthenticateUserUseCase authenticateUserUseCase = mock(AuthenticateUserUseCase.class);
        JwtTokenProvider tokenProvider = mock(JwtTokenProvider.class);
        AuthController controller = new AuthController(registerUserUseCase, authenticateUserUseCase, tokenProvider);

        when(registerUserUseCase.execute("alice", "secret123")).thenReturn("user-1");
        when(authenticateUserUseCase.execute("alice", "secret123")).thenReturn("jwt-token");
        when(tokenProvider.roleFromToken("jwt-token")).thenReturn("PLAYER");
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void register_returns201AndToken() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("alice", "secret123"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.role").value("PLAYER"));
    }

    @Test
    void login_returns200AndToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("alice", "secret123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.role").value("PLAYER"));
    }

    @Test
    void register_rejectsBlankUsername() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("", "secret123"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_rejectsShortPassword() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("alice", "ab"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_rejectsBlankFields() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("", ""))))
                .andExpect(status().isBadRequest());
    }
}
