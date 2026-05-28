package com.ulises.udiplomacy.application.service;

import com.ulises.udiplomacy.application.port.input.ListUserGamesUseCase;
import com.ulises.udiplomacy.application.port.output.GameProjectionRepository;
import com.ulises.udiplomacy.domain.user.GameReference;

import java.util.List;

public class ListUserGamesService implements ListUserGamesUseCase {
    private final GameProjectionRepository projectionRepository;

    public ListUserGamesService(GameProjectionRepository projectionRepository) {
        this.projectionRepository = projectionRepository;
    }

    @Override
    public List<GameReference> execute(String userId) {
        return projectionRepository.findByUserId(userId);
    }
}
