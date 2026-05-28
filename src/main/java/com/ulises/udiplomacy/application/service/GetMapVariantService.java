package com.ulises.udiplomacy.application.service;

import com.ulises.udiplomacy.application.port.input.GetMapVariantUseCase;
import com.ulises.udiplomacy.application.port.output.MapVariantRepository;
import com.ulises.udiplomacy.domain.game.MapVariant;

public class GetMapVariantService implements GetMapVariantUseCase {
    private final MapVariantRepository repository;

    public GetMapVariantService(MapVariantRepository repository) {
        this.repository = repository;
    }

    @Override
    public MapVariant execute(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Map variant not found: " + id));
    }
}
