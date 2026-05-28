package com.ulises.udiplomacy.application.service;

import com.ulises.udiplomacy.application.port.input.ExecuteOrdersUseCase;
import com.ulises.udiplomacy.application.port.output.EventPublisher;
import com.ulises.udiplomacy.application.port.output.GameProjectionRepository;
import com.ulises.udiplomacy.application.port.output.GameRepository;
import com.ulises.udiplomacy.domain.game.Game;
import com.ulises.udiplomacy.domain.game.ResolutionResult;
import com.ulises.udiplomacy.domain.game.events.DomainEvent;
import com.ulises.udiplomacy.domain.game.services.ConflictResolver;

public class ExecuteOrdersService implements ExecuteOrdersUseCase {
    private final GameRepository gameRepository;
    private final ConflictResolver conflictResolver;
    private final EventPublisher eventPublisher;
    private final GameProjectionRepository projectionRepository;

    public ExecuteOrdersService(GameRepository gameRepository,
                                 ConflictResolver conflictResolver,
                                 EventPublisher eventPublisher,
                                 GameProjectionRepository projectionRepository) {
        this.gameRepository = gameRepository;
        this.conflictResolver = conflictResolver;
        this.eventPublisher = eventPublisher;
        this.projectionRepository = projectionRepository;
    }

    @Override
    public ResolutionResult execute(String gameId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found: " + gameId));

        ResolutionResult result = game.executeOrders(conflictResolver);

        for (DomainEvent event : game.events()) {
            eventPublisher.publish(event);
        }
        game.clearEvents();

        gameRepository.save(game);
        return result;
    }
}
