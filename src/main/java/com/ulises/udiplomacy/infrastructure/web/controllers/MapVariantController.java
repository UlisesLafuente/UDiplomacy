package com.ulises.udiplomacy.infrastructure.web.controllers;

import com.ulises.udiplomacy.application.port.input.CreateMapVariantUseCase;
import com.ulises.udiplomacy.application.port.input.GetMapVariantSvgUseCase;
import com.ulises.udiplomacy.application.port.input.GetMapVariantUseCase;
import com.ulises.udiplomacy.application.port.input.ListMapVariantsUseCase;
import com.ulises.udiplomacy.infrastructure.web.dto.request.CreateMapVariantRequest;
import com.ulises.udiplomacy.infrastructure.web.dto.response.MapVariantResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class MapVariantController {
    private final ListMapVariantsUseCase listMapVariantsUseCase;
    private final GetMapVariantUseCase getMapVariantUseCase;
    private final CreateMapVariantUseCase createMapVariantUseCase;
    private final GetMapVariantSvgUseCase getMapVariantSvgUseCase;

    public MapVariantController(ListMapVariantsUseCase listMapVariantsUseCase,
                                 GetMapVariantUseCase getMapVariantUseCase,
                                 CreateMapVariantUseCase createMapVariantUseCase,
                                 GetMapVariantSvgUseCase getMapVariantSvgUseCase) {
        this.listMapVariantsUseCase = listMapVariantsUseCase;
        this.getMapVariantUseCase = getMapVariantUseCase;
        this.createMapVariantUseCase = createMapVariantUseCase;
        this.getMapVariantSvgUseCase = getMapVariantSvgUseCase;
    }

    @GetMapping("/api/maps")
    public ResponseEntity<List<MapVariantResponse>> listMaps() {
        var variants = listMapVariantsUseCase.execute();
        return ResponseEntity.ok(variants.stream().map(MapVariantResponse::summary).toList());
    }

    @GetMapping("/api/maps/{id}")
    public ResponseEntity<MapVariantResponse> getMap(@PathVariable String id) {
        var variant = getMapVariantUseCase.execute(id);
        return ResponseEntity.ok(MapVariantResponse.full(variant));
    }

    @PostMapping("/api/admin/maps")
    public ResponseEntity<MapVariantResponse> createMap(
            @Valid @RequestBody CreateMapVariantRequest request) {
        var variant = createMapVariantUseCase.execute(
                request.name(), request.mapJson(), request.svgContent());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(MapVariantResponse.full(variant));
    }

    @GetMapping("/api/maps/{id}/svg")
    public ResponseEntity<String> getMapSvg(@PathVariable String id) {
        String svg = getMapVariantSvgUseCase.execute(id);
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("image/svg+xml"))
                .body(svg);
    }
}
