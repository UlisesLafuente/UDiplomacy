package com.ulises.udiplomacy.infrastructure.web.controllers;

import com.ulises.udiplomacy.application.port.input.AuthenticateUserUseCase;
import com.ulises.udiplomacy.application.port.input.RegisterUserUseCase;
import com.ulises.udiplomacy.infrastructure.web.dto.request.LoginRequest;
import com.ulises.udiplomacy.infrastructure.web.dto.request.RegisterRequest;
import com.ulises.udiplomacy.infrastructure.web.dto.response.AuthResponse;
import com.ulises.udiplomacy.infrastructure.web.security.JwtTokenProvider;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        registerUserUseCase.execute(request.username(), request.password());
        String token = authenticateUserUseCase.execute(request.username(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthResponse(token, request.username(), "PLAYER"));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        String token = authenticateUserUseCase.execute(request.username(), request.password());
        String role = tokenProvider.roleFromToken(token);
        return ResponseEntity.ok(new AuthResponse(token, request.username(), role));
    }
}
