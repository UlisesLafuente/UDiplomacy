package com.ulises.udiplomacy.application.service;

import com.ulises.udiplomacy.application.port.input.AuthenticateUserUseCase;
import com.ulises.udiplomacy.application.port.output.PasswordEncoder;
import com.ulises.udiplomacy.application.port.output.TokenProvider;
import com.ulises.udiplomacy.application.port.output.UserRepository;
import com.ulises.udiplomacy.domain.user.User;

public class AuthenticateUserService implements AuthenticateUserUseCase {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;

    public AuthenticateUserService(UserRepository userRepository,
                                    PasswordEncoder passwordEncoder,
                                    TokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @Override
    public String execute(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!passwordEncoder.matches(password, user.passwordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        return tokenProvider.generateToken(user.userId(), user.role().name());
    }
}
