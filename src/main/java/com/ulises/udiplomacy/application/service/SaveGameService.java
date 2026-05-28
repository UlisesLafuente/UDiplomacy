package com.ulises.udiplomacy.application.service;

import com.ulises.udiplomacy.application.port.input.SaveGameUseCase;
import com.ulises.udiplomacy.application.port.output.EventPublisher;
import com.ulises.udiplomacy.application.port.output.GameRepository;
import com.ulises.udiplomacy.domain.game.Game;
import com.ulises.udiplomacy.domain.game.events.GameSaved;

public class SaveGameService implements SaveGameUseCase {
    private final GameRepository gameRepository;
    private final EventPublisher eventPublisher;

    public SaveGameService(GameRepository gameRepository, EventPublisher eventPublisher) {
        this.gameRepository = gameRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void execute(String gameId, String userId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found: " + gameId));
        gameRepository.save(game);
        eventPublisher.publish(new GameSaved(gameId, userId));
    }
}
