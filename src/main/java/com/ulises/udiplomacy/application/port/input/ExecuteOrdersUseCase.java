package com.ulises.udiplomacy.application.port.input;

import com.ulises.udiplomacy.domain.game.ResolutionResult;

public interface ExecuteOrdersUseCase {
    ResolutionResult execute(String gameId);
}
