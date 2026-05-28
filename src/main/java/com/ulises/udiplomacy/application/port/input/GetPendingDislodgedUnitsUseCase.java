package com.ulises.udiplomacy.application.port.input;

import com.ulises.udiplomacy.domain.game.DislodgementResult;

public interface GetPendingDislodgedUnitsUseCase {
    DislodgementResult execute(String gameId);
}
