package com.ulises.udiplomacy.application.service;

import com.ulises.udiplomacy.application.port.input.SubmitOrderUseCase;
import com.ulises.udiplomacy.application.port.output.GameRepository;
import com.ulises.udiplomacy.domain.game.Game;
import com.ulises.udiplomacy.domain.game.Order;
import com.ulises.udiplomacy.domain.game.services.OrderParser;

public class SubmitOrderService implements SubmitOrderUseCase {
    private final GameRepository gameRepository;
    private final OrderParser orderParser;

    public SubmitOrderService(GameRepository gameRepository, OrderParser orderParser) {
        this.gameRepository = gameRepository;
        this.orderParser = orderParser;
    }

    @Override
    public Order execute(String gameId, String rawOrder) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found: " + gameId));

        Order order = orderParser.parse(rawOrder, game.gameMap());
        game.submitOrder(order);
        gameRepository.save(game);
        return order;
    }
}
