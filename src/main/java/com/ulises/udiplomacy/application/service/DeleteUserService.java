package com.ulises.udiplomacy.application.service;

import com.ulises.udiplomacy.application.port.input.DeleteUserUseCase;
import com.ulises.udiplomacy.application.port.output.UserRepository;

public class DeleteUserService implements DeleteUserUseCase {
    private final UserRepository userRepository;

    public DeleteUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void execute(String userId, String requesterId) {
        if (userId.equals(requesterId)) {
            throw new IllegalArgumentException("Cannot delete your own admin account");
        }
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        userRepository.deleteById(user.userId());
    }
}
