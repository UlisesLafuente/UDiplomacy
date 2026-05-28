package com.ulises.udiplomacy.application.service;

import com.ulises.udiplomacy.application.port.input.GetGameUseCase;
import com.ulises.udiplomacy.application.port.output.GameRepository;
import com.ulises.udiplomacy.domain.game.Game;

public class GetGameService implements GetGameUseCase {
    private final GameRepository gameRepository;

    public GetGameService(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    @Override
    public Game execute(String gameId) {
        return gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found: " + gameId));
    }
}
