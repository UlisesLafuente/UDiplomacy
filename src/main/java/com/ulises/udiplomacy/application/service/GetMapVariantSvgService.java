package com.ulises.udiplomacy.application.service;

import com.ulises.udiplomacy.application.port.input.GetMapVariantSvgUseCase;
import com.ulises.udiplomacy.application.port.output.MapVariantRepository;

public class GetMapVariantSvgService implements GetMapVariantSvgUseCase {
    private final MapVariantRepository repository;

    public GetMapVariantSvgService(MapVariantRepository repository) {
        this.repository = repository;
    }

    @Override
    public String execute(String id) {
        var variant = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Map variant not found: " + id));
        String svg = variant.svgContent();
        if (svg == null || svg.isBlank()) {
            throw new IllegalArgumentException("Map variant '" + id + "' has no SVG content");
        }
        return svg;
    }
}
