package com.ulises.udiplomacy.application.service;

import com.ulises.udiplomacy.application.port.input.GetGameHistoryUseCase;
import com.ulises.udiplomacy.application.port.output.GameRepository;
import com.ulises.udiplomacy.domain.game.Game;

public class GetGameHistoryService implements GetGameHistoryUseCase {
    private final GameRepository gameRepository;

    public GetGameHistoryService(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    @Override
    public Game execute(String gameId) {
        return gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found: " + gameId));
    }
}
