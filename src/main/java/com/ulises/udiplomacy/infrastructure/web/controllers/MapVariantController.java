package com.ulises.udiplomacy.infrastructure.web.controllers;

import com.ulises.udiplomacy.application.port.input.CreateMapVariantUseCase;
import com.ulises.udiplomacy.application.port.input.GetMapVariantSvgUseCase;
import com.ulises.udiplomacy.application.port.input.GetMapVariantUseCase;
import com.ulises.udiplomacy.application.port.input.ListMapVariantsUseCase;
import com.ulises.udiplomacy.application.port.output.MapVariantRepository;
import com.ulises.udiplomacy.infrastructure.web.dto.response.MapVariantResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
public class MapVariantController {
    private final ListMapVariantsUseCase listMapVariantsUseCase;
    private final GetMapVariantUseCase getMapVariantUseCase;
    private final CreateMapVariantUseCase createMapVariantUseCase;
    private final GetMapVariantSvgUseCase getMapVariantSvgUseCase;
    private final MapVariantRepository mapVariantRepository;

    public MapVariantController(ListMapVariantsUseCase listMapVariantsUseCase,
                                 GetMapVariantUseCase getMapVariantUseCase,
                                 CreateMapVariantUseCase createMapVariantUseCase,
                                 GetMapVariantSvgUseCase getMapVariantSvgUseCase,
                                 MapVariantRepository mapVariantRepository) {
        this.listMapVariantsUseCase = listMapVariantsUseCase;
        this.getMapVariantUseCase = getMapVariantUseCase;
        this.createMapVariantUseCase = createMapVariantUseCase;
        this.getMapVariantSvgUseCase = getMapVariantSvgUseCase;
        this.mapVariantRepository = mapVariantRepository;
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

    @PostMapping(value = "/api/admin/maps", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MapVariantResponse> createMap(
            @RequestParam("name") String name,
            @RequestParam("mapJson") MultipartFile mapJsonFile,
            @RequestParam(value = "svgContent", required = false) MultipartFile svgContentFile) throws IOException {
        String mapJson = new String(mapJsonFile.getBytes());
        String svgContent = svgContentFile != null ? new String(svgContentFile.getBytes()) : null;
        var variant = createMapVariantUseCase.execute(name, mapJson, svgContent);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(MapVariantResponse.full(variant));
    }

    private static final String DEFAULT_MAP_ID = "europe-classic";

    @DeleteMapping("/api/admin/maps/{id}")
    public ResponseEntity<Void> deleteMap(@PathVariable String id) {
        if (DEFAULT_MAP_ID.equals(id)) {
            return ResponseEntity.badRequest().build();
        }
        mapVariantRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/maps/{id}/svg")
    public ResponseEntity<String> getMapSvg(@PathVariable String id) {
        String svg = getMapVariantSvgUseCase.execute(id);
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("image/svg+xml"))
                .body(svg);
    }
}
