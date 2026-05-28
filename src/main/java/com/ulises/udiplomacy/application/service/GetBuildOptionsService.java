package com.ulises.udiplomacy.application.service;

import com.ulises.udiplomacy.application.port.input.GetBuildOptionsUseCase;
import com.ulises.udiplomacy.application.port.output.GameRepository;
import com.ulises.udiplomacy.domain.game.BuildCapacity;
import com.ulises.udiplomacy.domain.game.Game;
import com.ulises.udiplomacy.domain.game.Nation;

public class GetBuildOptionsService implements GetBuildOptionsUseCase {
    private final GameRepository gameRepository;

    public GetBuildOptionsService(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    @Override
    public BuildCapacity execute(String gameId, Nation nation) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found: " + gameId));
        return game.getBuildOptions(nation);
    }
}
