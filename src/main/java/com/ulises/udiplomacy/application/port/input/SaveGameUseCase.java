package com.ulises.udiplomacy.application.port.input;

public interface SaveGameUseCase {
    void execute(String gameId, String userId);
}
