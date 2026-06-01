package com.ulises.udiplomacy.application.service;

import com.ulises.udiplomacy.application.port.input.DeleteGameUseCase;
import com.ulises.udiplomacy.application.port.output.GameProjectionRepository;
import com.ulises.udiplomacy.application.port.output.GameRepository;

public class DeleteGameService implements DeleteGameUseCase {
    private final GameRepository gameRepository;
    private final GameProjectionRepository projectionRepository;

    public DeleteGameService(GameRepository gameRepository,
                              GameProjectionRepository projectionRepository) {
        this.gameRepository = gameRepository;
        this.projectionRepository = projectionRepository;
    }

    @Override
    public void execute(String gameId) {
        if (!gameRepository.existsById(gameId)) {
            throw new IllegalArgumentException("Game not found: " + gameId);
        }
        gameRepository.deleteById(gameId);
        projectionRepository.deleteByGameId(gameId);
    }
}
