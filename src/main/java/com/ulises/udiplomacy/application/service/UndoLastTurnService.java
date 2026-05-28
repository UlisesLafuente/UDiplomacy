package com.ulises.udiplomacy.application.service;

import com.ulises.udiplomacy.application.port.input.UndoLastTurnUseCase;
import com.ulises.udiplomacy.application.port.output.GameRepository;
import com.ulises.udiplomacy.domain.game.Game;

public class UndoLastTurnService implements UndoLastTurnUseCase {
    private final GameRepository gameRepository;

    public UndoLastTurnService(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    @Override
    public void execute(String gameId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found: " + gameId));
        game.undoLastTurn();
        gameRepository.save(game);
    }
}
