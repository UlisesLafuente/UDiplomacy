package com.ulises.udiplomacy.application.port.input;

import com.ulises.udiplomacy.domain.game.Order;

public interface SubmitOrderUseCase {
    Order execute(String gameId, String rawOrder);
}
