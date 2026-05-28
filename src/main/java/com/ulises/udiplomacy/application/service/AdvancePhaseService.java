package com.ulises.udiplomacy.application.service;

import com.ulises.udiplomacy.application.port.input.AdvancePhaseUseCase;
import com.ulises.udiplomacy.application.port.output.GameRepository;
import com.ulises.udiplomacy.domain.game.Game;

public class AdvancePhaseService implements AdvancePhaseUseCase {
    private final GameRepository gameRepository;

    public AdvancePhaseService(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    @Override
    public void execute(String gameId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found: " + gameId));
        game.advancePhase();
        gameRepository.save(game);
    }
}
