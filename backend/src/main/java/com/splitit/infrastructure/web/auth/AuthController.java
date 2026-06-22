package com.splitit.infrastructure.web.auth;

import com.splitit.domain.user.model.User;
import com.splitit.domain.user.port.in.AuthenticateUserUseCase;
import com.splitit.domain.user.port.in.RegisterUserUseCase;
import com.splitit.infrastructure.security.JwtTokenProvider;
import com.splitit.infrastructure.web.auth.dto.AuthResponse;
import com.splitit.infrastructure.web.auth.dto.LoginRequest;
import com.splitit.infrastructure.web.auth.dto.RegisterRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final RegisterUserUseCase registerUserUseCase;
    private final AuthenticateUserUseCase authenticateUserUseCase;
    private final JwtTokenProvider tokenProvider;

    public AuthController(RegisterUserUseCase registerUserUseCase,
                          AuthenticateUserUseCase authenticateUserUseCase,
                          JwtTokenProvider tokenProvider) {
        this.registerUserUseCase = registerUserUseCase;
        this.authenticateUserUseCase = authenticateUserUseCase;
        this.tokenProvider = tokenProvider;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        User user = registerUserUseCase.register(new RegisterUserUseCase.RegisterCommand(
                request.email(), request.displayName(), request.password()));
        return new AuthResponse(tokenProvider.generateToken(user));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        User user = authenticateUserUseCase.authenticate(new AuthenticateUserUseCase.AuthenticateCommand(
                request.email(), request.password()));
        return new AuthResponse(tokenProvider.generateToken(user));
    }
}
