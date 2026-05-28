package com.ulises.udiplomacy.application.service;

import com.ulises.udiplomacy.application.port.input.RewindGameUseCase;
import com.ulises.udiplomacy.application.port.output.GameRepository;
import com.ulises.udiplomacy.domain.game.Game;

public class RewindGameService implements RewindGameUseCase {
    private final GameRepository gameRepository;

    public RewindGameService(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    @Override
    public void execute(String gameId, int turnIndex) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found: " + gameId));
        game.rewindToTurn(turnIndex);
        gameRepository.save(game);
    }
}
