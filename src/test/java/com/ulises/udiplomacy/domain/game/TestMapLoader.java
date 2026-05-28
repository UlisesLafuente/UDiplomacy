package com.ulises.udiplomacy.domain.game;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ulises.udiplomacy.domain.game.enums.ProvinceType;

import java.io.InputStream;
import java.util.*;

public final class TestMapLoader {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static GameMap loadClassic() {
        try (InputStream is = TestMapLoader.class.getClassLoader()
                .getResourceAsStream("europe-classic.json")) {
            if (is == null) throw new IllegalStateException("europe-classic.json not found");
            Map<String, Object> root = MAPPER.readValue(is, new TypeReference<>() {});
            return parseMap(root);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load test map", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static GameMap parseMap(Map<String, Object> root) {
        List<Map<String, Object>> provincesRaw = (List<Map<String, Object>>) root.get("provinces");
        List<Province> provinces = new ArrayList<>();

        for (Map<String, Object> p : provincesRaw) {
            String name = (String) p.get("name");
            ProvinceType type = ProvinceType.valueOf((String) p.get("type"));
            String homeNationStr = (String) p.get("homeNation");
            Nation homeNation = homeNationStr != null ? new Nation(homeNationStr) : null;
            boolean supplyCenter = Boolean.TRUE.equals(p.get("supplyCenter"));
            List<String> coastsRaw = (List<String>) p.get("coasts");
            List<Coast> coasts = coastsRaw != null
                    ? coastsRaw.stream().map(Coast::new).toList()
                    : List.of();
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
        return new GameMap(id != null ? id : "test", name != null ? name : "Test Map", provinces);
    }
}
