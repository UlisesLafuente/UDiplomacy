package com.ulises.udiplomacy.application.port.input;

public interface DeleteUserUseCase {
    void execute(String userId, String requesterId);
}
