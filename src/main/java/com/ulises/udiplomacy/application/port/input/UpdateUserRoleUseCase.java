package com.ulises.udiplomacy.application.port.input;

public interface UpdateUserRoleUseCase {
    void execute(String userId, String newRole);
}
