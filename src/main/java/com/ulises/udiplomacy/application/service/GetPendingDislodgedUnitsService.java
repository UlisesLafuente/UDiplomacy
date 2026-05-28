package com.ulises.udiplomacy.application.service;

import com.ulises.udiplomacy.application.port.input.GetPendingDislodgedUnitsUseCase;
import com.ulises.udiplomacy.application.port.output.GameRepository;
import com.ulises.udiplomacy.domain.game.DislodgementResult;
import com.ulises.udiplomacy.domain.game.Game;

public class GetPendingDislodgedUnitsService implements GetPendingDislodgedUnitsUseCase {
    private final GameRepository gameRepository;

    public GetPendingDislodgedUnitsService(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    @Override
    public DislodgementResult execute(String gameId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found: " + gameId));
        return game.dislodgementResult()
                .orElseThrow(() -> new IllegalStateException("No dislodged units pending"));
    }
}
