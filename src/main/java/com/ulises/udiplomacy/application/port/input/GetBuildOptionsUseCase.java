package com.ulises.udiplomacy.application.port.input;

import com.ulises.udiplomacy.domain.game.BuildCapacity;
import com.ulises.udiplomacy.domain.game.Nation;

public interface GetBuildOptionsUseCase {
    BuildCapacity execute(String gameId, Nation nation);
}
