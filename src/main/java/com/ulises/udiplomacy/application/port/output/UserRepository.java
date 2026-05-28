package com.ulises.udiplomacy.application.port.output;

import com.ulises.udiplomacy.domain.user.User;

import java.util.Optional;

public interface UserRepository {
    void save(User user);
    Optional<User> findById(String userId);
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
}
