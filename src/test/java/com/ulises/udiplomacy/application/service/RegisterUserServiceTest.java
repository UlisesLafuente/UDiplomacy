package com.ulises.udiplomacy.application.service;

import com.ulises.udiplomacy.application.port.output.PasswordEncoder;
import com.ulises.udiplomacy.application.port.output.UserRepository;
import com.ulises.udiplomacy.domain.user.Role;
import com.ulises.udiplomacy.domain.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegisterUserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private RegisterUserService service;

    @Captor private ArgumentCaptor<User> userCaptor;

    @Test
    void registersUser() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(passwordEncoder.encode("secret")).thenReturn("hashed-secret");

        String userId = service.execute("alice", "secret");

        assertNotNull(userId);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertEquals("alice", saved.username());
        assertEquals("hashed-secret", saved.passwordHash());
        assertEquals(Role.PLAYER, saved.role());
    }

    @Test
    void rejectsDuplicateUsername() {
        when(userRepository.existsByUsername("alice")).thenReturn(true);
        assertThrows(IllegalArgumentException.class,
                () -> service.execute("alice", "secret"));
        verify(userRepository, never()).save(any());
    }
}
