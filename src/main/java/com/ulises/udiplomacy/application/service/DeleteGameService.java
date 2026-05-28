package com.ulises.udiplomacy.application.service;

import com.ulises.udiplomacy.application.port.input.DeleteGameUseCase;
import com.ulises.udiplomacy.application.port.output.GameRepository;

public class DeleteGameService implements DeleteGameUseCase {
    private final GameRepository gameRepository;

    public DeleteGameService(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    @Override
    public void execute(String gameId) {
        if (!gameRepository.existsById(gameId)) {
            throw new IllegalArgumentException("Game not found: " + gameId);
        }
        gameRepository.deleteById(gameId);
    }
}
