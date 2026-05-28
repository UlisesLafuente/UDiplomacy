package com.ulises.udiplomacy.application.port.input;

import com.ulises.udiplomacy.domain.game.Game;

public interface CreateGameUseCase {
    Game execute(String mapId, String mapJson, String userId);
}
