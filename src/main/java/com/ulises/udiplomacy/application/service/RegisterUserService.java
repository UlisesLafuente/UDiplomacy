package com.ulises.udiplomacy.application.service;

import com.ulises.udiplomacy.application.port.input.RegisterUserUseCase;
import com.ulises.udiplomacy.application.port.output.PasswordEncoder;
import com.ulises.udiplomacy.application.port.output.UserRepository;
import com.ulises.udiplomacy.domain.user.Role;
import com.ulises.udiplomacy.domain.user.User;

import java.util.UUID;

public class RegisterUserService implements RegisterUserUseCase {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public RegisterUserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public String execute(String username, String password, String role) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already taken");
        }

        String userId = UUID.randomUUID().toString();
        String hashedPassword = passwordEncoder.encode(password);
        Role userRole = role != null && role.equalsIgnoreCase("ADMIN") ? Role.ADMIN : Role.PLAYER;

        User user = new User(userId, username, hashedPassword, userRole);
        userRepository.save(user);
        return userId;
    }
}
