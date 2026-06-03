package com.ulises.udiplomacy.application.service;

import com.ulises.udiplomacy.application.port.input.ListAllGamesUseCase;
import com.ulises.udiplomacy.application.port.output.GameProjectionRepository;
import com.ulises.udiplomacy.domain.user.GameReference;

import java.util.List;

public class ListAllGamesService implements ListAllGamesUseCase {
    private final GameProjectionRepository projectionRepository;

    public ListAllGamesService(GameProjectionRepository projectionRepository) {
        this.projectionRepository = projectionRepository;
    }

    @Override
    public List<GameReference> execute() {
        return projectionRepository.findAll();
    }
}
