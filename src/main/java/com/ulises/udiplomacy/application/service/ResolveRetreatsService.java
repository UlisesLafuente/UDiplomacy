package com.ulises.udiplomacy.application.service;

import com.ulises.udiplomacy.application.port.input.ResolveRetreatsUseCase;
import com.ulises.udiplomacy.application.port.output.GameRepository;
import com.ulises.udiplomacy.domain.game.Game;
import com.ulises.udiplomacy.domain.game.Order;
import com.ulises.udiplomacy.domain.game.services.ConflictResolver;
import com.ulises.udiplomacy.domain.game.services.OrderParser;

import java.util.List;

public class ResolveRetreatsService implements ResolveRetreatsUseCase {
    private final GameRepository gameRepository;
    private final OrderParser orderParser;
    private final ConflictResolver conflictResolver;

    public ResolveRetreatsService(GameRepository gameRepository,
                                   OrderParser orderParser,
                                   ConflictResolver conflictResolver) {
        this.gameRepository = gameRepository;
        this.orderParser = orderParser;
        this.conflictResolver = conflictResolver;
    }

    @Override
    public void execute(String gameId, List<String> rawOrders) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found: " + gameId));

        List<Order> orders = rawOrders.stream()
                .map(raw -> orderParser.parse(raw, game.gameMap()))
                .toList();

        game.resolveRetreats(orders, conflictResolver);
        gameRepository.save(game);
    }
}
