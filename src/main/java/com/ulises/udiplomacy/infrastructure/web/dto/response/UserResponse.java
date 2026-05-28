package com.ulises.udiplomacy.infrastructure.web.dto.response;

import com.ulises.udiplomacy.domain.user.User;

public record UserResponse(String userId, String username, String role) {
    public static UserResponse from(User user) {
        return new UserResponse(user.userId(), user.username(), user.role().name());
    }
}
