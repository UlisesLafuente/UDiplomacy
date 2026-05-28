package com.ulises.udiplomacy.application.service;

import com.ulises.udiplomacy.application.port.input.UpdateUserRoleUseCase;
import com.ulises.udiplomacy.application.port.output.UserRepository;
import com.ulises.udiplomacy.domain.user.Role;
import com.ulises.udiplomacy.domain.user.User;

public class UpdateUserRoleService implements UpdateUserRoleUseCase {
    private final UserRepository repository;

    public UpdateUserRoleService(UserRepository repository) {
        this.repository = repository;
    }

    @Override
    public void execute(String userId, String newRole) {
        User user = repository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        Role role;
        try {
            role = Role.valueOf(newRole.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role: " + newRole + ". Must be PLAYER or ADMIN");
        }

        if (user.role() == role) return;

        User updated = new User(user.userId(), user.username(), user.passwordHash(), role, user.gameReferences());
        repository.save(updated);
    }
}
