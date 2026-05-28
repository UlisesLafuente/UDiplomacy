package com.ulises.udiplomacy.application.service;

import com.ulises.udiplomacy.application.port.output.PasswordEncoder;
import com.ulises.udiplomacy.application.port.output.TokenProvider;
import com.ulises.udiplomacy.application.port.output.UserRepository;
import com.ulises.udiplomacy.domain.user.Role;
import com.ulises.udiplomacy.domain.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticateUserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private TokenProvider tokenProvider;
    @InjectMocks private AuthenticateUserService service;

    @Test
    void authenticatesUser() {
        User user = new User("uid-1", "alice", "hashed-secret", Role.PLAYER);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "hashed-secret")).thenReturn(true);
        when(tokenProvider.generateToken("uid-1", "PLAYER")).thenReturn("jwt-token");

        String token = service.execute("alice", "secret");

        assertEquals("jwt-token", token);
    }

    @Test
    void rejectsUnknownUsername() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> service.execute("unknown", "secret"));
    }

    @Test
    void rejectsWrongPassword() {
        User user = new User("uid-1", "alice", "hashed-secret", Role.PLAYER);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed-secret")).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
                () -> service.execute("alice", "wrong"));
    }
}
