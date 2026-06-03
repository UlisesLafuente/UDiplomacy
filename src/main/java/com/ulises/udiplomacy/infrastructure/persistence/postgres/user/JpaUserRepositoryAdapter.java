package com.ulises.udiplomacy.infrastructure.persistence.postgres.user;

import com.ulises.udiplomacy.application.port.output.UserRepository;
import com.ulises.udiplomacy.domain.user.Role;
import com.ulises.udiplomacy.domain.user.User;

import java.util.List;
import java.util.Optional;

public class JpaUserRepositoryAdapter implements UserRepository {
    private final SpringDataJpaUserRepository springRepository;

    public JpaUserRepositoryAdapter(SpringDataJpaUserRepository springRepository) {
        this.springRepository = springRepository;
    }

    @Override
    public void save(User user) {
        springRepository.save(new UserEntity(
                user.userId(), user.username(), user.passwordHash(), user.role().name()));
    }

    @Override
    public Optional<User> findById(String userId) {
        return springRepository.findById(userId).map(this::toDomain);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return springRepository.findByUsername(username).map(this::toDomain);
    }

    @Override
    public boolean existsByUsername(String username) {
        return springRepository.existsByUsername(username);
    }

    @Override
    public List<User> findAll() {
        return springRepository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public void deleteById(String userId) {
        springRepository.deleteById(userId);
    }

    private User toDomain(UserEntity entity) {
        return new User(entity.getUserId(), entity.getUsername(),
                entity.getPasswordHash(), Role.valueOf(entity.getRole()));
    }
}
