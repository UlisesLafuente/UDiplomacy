package com.ulises.udiplomacy.application.port.input;

public interface RewindGameUseCase {
    void execute(String gameId, int turnIndex);
}
