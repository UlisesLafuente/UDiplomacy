package com.ulises.udiplomacy.application.service;

import com.ulises.udiplomacy.application.port.input.CreateMapVariantUseCase;
import com.ulises.udiplomacy.application.port.output.MapVariantRepository;
import com.ulises.udiplomacy.domain.game.MapVariant;

import java.time.Instant;
import java.util.UUID;

public class CreateMapVariantService implements CreateMapVariantUseCase {
    private final MapVariantRepository repository;
    private final SvgValidator svgValidator;

    public CreateMapVariantService(MapVariantRepository repository, SvgValidator svgValidator) {
        this.repository = repository;
        this.svgValidator = svgValidator;
    }

    @Override
    public MapVariant execute(String name, String mapJson, String svgContent, boolean colonialRule) {
        svgValidator.validate(svgContent, mapJson);
        String id = UUID.randomUUID().toString();
        MapVariant variant = new MapVariant(id, name, mapJson, svgContent, colonialRule, Instant.now());
        repository.save(variant);
        return variant;
    }
}
