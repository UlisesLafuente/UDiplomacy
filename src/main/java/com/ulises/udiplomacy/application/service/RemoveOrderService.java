package com.ulises.udiplomacy.application.service;

import com.ulises.udiplomacy.application.port.input.RemoveOrderUseCase;
import com.ulises.udiplomacy.application.port.output.GameRepository;
import com.ulises.udiplomacy.domain.game.Game;
import com.ulises.udiplomacy.domain.game.Order;

public class RemoveOrderService implements RemoveOrderUseCase {
    private final GameRepository gameRepository;

    public RemoveOrderService(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    @Override
    public Order execute(String gameId, int orderIndex) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found: " + gameId));
        Order removed = game.removeOrder(orderIndex);
        gameRepository.save(game);
        return removed;
    }
}
