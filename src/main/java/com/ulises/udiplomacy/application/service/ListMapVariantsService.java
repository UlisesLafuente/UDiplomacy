package com.ulises.udiplomacy.application.service;

import com.ulises.udiplomacy.application.port.input.ListMapVariantsUseCase;
import com.ulises.udiplomacy.application.port.output.MapVariantRepository;
import com.ulises.udiplomacy.domain.game.MapVariant;

import java.util.List;

public class ListMapVariantsService implements ListMapVariantsUseCase {
    private final MapVariantRepository repository;

    public ListMapVariantsService(MapVariantRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<MapVariant> execute() {
        return repository.findAll();
    }
}
