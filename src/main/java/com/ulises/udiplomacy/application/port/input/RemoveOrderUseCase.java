package com.ulises.udiplomacy.application.port.input;

import com.ulises.udiplomacy.domain.game.Order;

public interface RemoveOrderUseCase {
    Order execute(String gameId, int orderIndex);
}
