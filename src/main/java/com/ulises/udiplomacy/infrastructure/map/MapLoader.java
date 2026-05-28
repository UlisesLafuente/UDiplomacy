package com.ulises.udiplomacy.infrastructure.map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ulises.udiplomacy.domain.game.*;
import com.ulises.udiplomacy.domain.game.enums.ProvinceType;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class MapLoader {
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;

    public MapLoader(ResourceLoader resourceLoader, ObjectMapper objectMapper) {
        this.resourceLoader = resourceLoader;
        this.objectMapper = objectMapper;
    }

    public GameMap loadMap(String mapId, String path) {
        try {
            Resource resource = resourceLoader.getResource("classpath:" + path);
            try (InputStream is = resource.getInputStream()) {
                Map<String, Object> root = objectMapper.readValue(is, new TypeReference<>() {});
                return parseMap(root);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load map: " + path, e);
        }
    }

    private GameMap parseMap(Map<String, Object> root) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> provincesRaw = (List<Map<String, Object>>) root.get("provinces");
        List<Province> provinces = new ArrayList<>();

        for (Map<String, Object> p : provincesRaw) {
            String name = (String) p.get("name");
            ProvinceType type = ProvinceType.valueOf((String) p.get("type"));
            String homeNationStr = (String) p.get("homeNation");
            Nation homeNation = homeNationStr != null ? new Nation(homeNationStr) : null;
            boolean supplyCenter = Boolean.TRUE.equals(p.get("supplyCenter"));

            @SuppressWarnings("unchecked")
            List<String> coastsRaw = (List<String>) p.get("coasts");
            List<Coast> coasts = coastsRaw != null
                    ? coastsRaw.stream().map(Coast::new).toList()
                    : List.of();

            @SuppressWarnings("unchecked")
            Map<String, String> adjRaw = (Map<String, String>) p.get("adjacencies");
            Map<String, Coast> adjacencies = new HashMap<>();
            if (adjRaw != null) {
                for (var entry : adjRaw.entrySet()) {
                    adjacencies.put(entry.getKey(),
                            entry.getValue() != null ? new Coast(entry.getValue()) : null);
                }
            }

            provinces.add(new Province(name, type, homeNation, supplyCenter, coasts, adjacencies));
        }

        String id = (String) root.get("id");
        String name = (String) root.get("name");
        return new GameMap(id, name, provinces);
    }
}
