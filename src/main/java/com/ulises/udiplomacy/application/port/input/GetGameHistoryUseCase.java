package com.ulises.udiplomacy.application.port.input;

import com.ulises.udiplomacy.domain.game.Game;

public interface GetGameHistoryUseCase {
    Game execute(String gameId);
}
