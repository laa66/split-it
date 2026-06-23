package com.splitit.infrastructure.web.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.splitit.domain.group.port.in.ClaimPendingInvitationsUseCase;
import com.splitit.domain.user.exception.EmailAlreadyUsedException;
import com.splitit.domain.user.exception.InvalidCredentialsException;
import com.splitit.domain.user.model.User;
import com.splitit.domain.user.port.in.AuthenticateUserUseCase;
import com.splitit.domain.user.port.in.RegisterUserUseCase;
import com.splitit.infrastructure.security.JwtTokenProvider;
import com.splitit.infrastructure.web.shared.GlobalExceptionHandler;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RegisterUserUseCase registerUserUseCase;

    @MockBean
    private AuthenticateUserUseCase authenticateUserUseCase;

    @MockBean
    private JwtTokenProvider tokenProvider;

    @MockBean
    private ClaimPendingInvitationsUseCase claimPendingInvitationsUseCase;

    private User sampleUser() {
        return new User(UUID.randomUUID(), "alice@example.com", "Alice", "HASH", OffsetDateTime.now());
    }

    @Test
    void register_ok_returns200WithToken() throws Exception {
        when(registerUserUseCase.register(any())).thenReturn(sampleUser());
        when(tokenProvider.generateToken(any())).thenReturn("jwt-token");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"alice@example.com","displayName":"Alice","password":"password123"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    void register_emailTaken_returns409InErrorFormat() throws Exception {
        when(registerUserUseCase.register(any()))
                .thenThrow(new EmailAlreadyUsedException("alice@example.com"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"alice@example.com","displayName":"Alice","password":"password123"}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void register_shortPassword_returns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"alice@example.com","displayName":"Alice","password":"short"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors", org.hamcrest.Matchers.not(org.hamcrest.Matchers.empty())));
    }

    @Test
    void register_passwordOver72Chars_returns400NotServerError() throws Exception {
        String longPassword = "a".repeat(73);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"alice@example.com","displayName":"Alice","password":"%s"}"""
                                .formatted(longPassword)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void login_ok_returns200WithToken() throws Exception {
        when(authenticateUserUseCase.authenticate(any())).thenReturn(sampleUser());
        when(tokenProvider.generateToken(any())).thenReturn("jwt-token");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"alice@example.com","password":"password123"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    void login_badCredentials_returns401WithGenericMessage() throws Exception {
        when(authenticateUserUseCase.authenticate(any()))
                .thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"alice@example.com","password":"wrongpass"}"""))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid email or password"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void malformedJson_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ this is not valid json "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Malformed request body"));
    }
}
