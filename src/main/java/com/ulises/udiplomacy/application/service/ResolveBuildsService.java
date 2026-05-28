package com.ulises.udiplomacy.application.service;

import com.ulises.udiplomacy.application.port.input.ResolveBuildsUseCase;
import com.ulises.udiplomacy.application.port.output.EventPublisher;
import com.ulises.udiplomacy.application.port.output.GameRepository;
import com.ulises.udiplomacy.domain.game.Game;
import com.ulises.udiplomacy.domain.game.Order;
import com.ulises.udiplomacy.domain.game.events.DomainEvent;
import com.ulises.udiplomacy.domain.game.services.OrderParser;

import java.util.List;

public class ResolveBuildsService implements ResolveBuildsUseCase {
    private final GameRepository gameRepository;
    private final OrderParser orderParser;
    private final EventPublisher eventPublisher;

    public ResolveBuildsService(GameRepository gameRepository,
                                 OrderParser orderParser,
                                 EventPublisher eventPublisher) {
        this.gameRepository = gameRepository;
        this.orderParser = orderParser;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void execute(String gameId, List<String> rawOrders) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found: " + gameId));

        List<Order> orders = rawOrders.stream()
                .map(raw -> orderParser.parse(raw, game.gameMap()))
                .toList();

        game.resolveBuilds(orders);

        for (DomainEvent event : game.events()) {
            eventPublisher.publish(event);
        }
        game.clearEvents();

        gameRepository.save(game);
    }
}
