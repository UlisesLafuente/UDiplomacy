package com.ulises.udiplomacy.application.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ulises.udiplomacy.application.port.input.CreateGameUseCase;
import com.ulises.udiplomacy.application.port.output.GameRepository;
import com.ulises.udiplomacy.domain.game.*;
import com.ulises.udiplomacy.domain.game.enums.GameState;
import com.ulises.udiplomacy.domain.game.enums.ProvinceType;
import com.ulises.udiplomacy.domain.game.enums.UnitType;
import com.ulises.udiplomacy.domain.user.GameReference;
import com.ulises.udiplomacy.application.port.output.GameProjectionRepository;
import com.ulises.udiplomacy.application.port.output.MapVariantRepository;
import com.ulises.udiplomacy.application.port.output.UserRepository;
import com.ulises.udiplomacy.domain.user.Role;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class CreateGameService implements CreateGameUseCase {
    private final GameRepository gameRepository;
    private final GameProjectionRepository projectionRepository;
    private final MapVariantRepository mapVariantRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public CreateGameService(GameRepository gameRepository,
                             GameProjectionRepository projectionRepository,
                             MapVariantRepository mapVariantRepository,
                             UserRepository userRepository,
                             ObjectMapper objectMapper) {
        this.gameRepository = gameRepository;
        this.projectionRepository = projectionRepository;
        this.mapVariantRepository = mapVariantRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public Game execute(String mapId, String mapJson, String userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        if (user.role() == Role.ADMIN) {
            throw new IllegalArgumentException("Admin accounts cannot create games");
        }
        try {
            String json = resolveMapJson(mapId, mapJson);
            Map<String, Object> root = objectMapper.readValue(json, new TypeReference<>() {});
            GameMap gameMap = parseMap(root);
            Set<Nation> nations = extractNations(gameMap);

            String gameId = UUID.randomUUID().toString();
            Game game = new Game(gameId, gameMap, nations);

            List<Unit> initialUnits = parseInitialUnits(root);
            game.start(initialUnits);
            gameRepository.save(game);

            projectionRepository.saveGameReference(new GameReference(
                    gameId, userId, user.username(), gameMap.name(), GameState.IN_PROGRESS,
                    Instant.now(), Instant.now()));

            return game;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create game", e);
        }
    }

    private String resolveMapJson(String mapId, String mapJson) {
        if (mapId != null) {
            return mapVariantRepository.findById(mapId)
                    .orElseThrow(() -> new IllegalArgumentException("Map variant not found: " + mapId))
                    .mapJson();
        }
        return mapJson;
    }

    @SuppressWarnings("unchecked")
    private GameMap parseMap(Map<String, Object> root) {
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
        return new GameMap(id != null ? id : "europe-classic",
                name != null ? name : "Classic Diplomacy Europe", provinces);
    }

    @SuppressWarnings("unchecked")
    private List<Unit> parseInitialUnits(Map<String, Object> root) {
        List<Map<String, Object>> initialUnitsRaw = (List<Map<String, Object>>) root.get("initialUnits");
        if (initialUnitsRaw == null) return List.of();

        List<Unit> units = new ArrayList<>();
        for (Map<String, Object> nationGroup : initialUnitsRaw) {
            List<Map<String, Object>> rawUnits = (List<Map<String, Object>>) nationGroup.get("units");
            if (rawUnits == null) continue;
            for (Map<String, Object> rawUnit : rawUnits) {
                Nation nation = new Nation((String) rawUnit.get("nation"));
                UnitType unitType = UnitType.valueOf((String) rawUnit.get("unitType"));
                Territory location = new Territory((String) rawUnit.get("province"));
                units.add(new Unit(unitType, nation, location));
            }
        }
        return units;
    }

    private Set<Nation> extractNations(GameMap gameMap) {
        return gameMap.provinces().values().stream()
                .map(Province::homeNation)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());
    }


}
