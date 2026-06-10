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
            boolean colonialRule = false;
            String json;
            if (mapId != null) {
                var variant = mapVariantRepository.findById(mapId)
                        .orElseThrow(() -> new IllegalArgumentException("Map variant not found: " + mapId));
                json = variant.mapJson();
                colonialRule = variant.colonialRule();
            } else {
                json = mapJson;
            }
            Map<String, Object> root = objectMapper.readValue(json, new TypeReference<>() {});
            GameMap gameMap = parseMap(root, mapId);
            Set<Nation> nations = extractNations(gameMap);

            String gameId = UUID.randomUUID().toString();
            Game game = new Game(gameId, gameMap, nations, colonialRule);

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

    private GameMap parseMap(Map<String, Object> root, String mongoId) {
        Object provincesObj = root.get("provinces");
        if (!(provincesObj instanceof List<?> provincesRaw)) {
            throw new IllegalArgumentException("Map JSON must contain a 'provinces' array");
        }
        List<Province> provinces = new ArrayList<>();

        for (Object provinceObj : provincesRaw) {
            if (!(provinceObj instanceof Map<?, ?> p)) continue;
            String name = (String) p.get("name");
            ProvinceType type = ProvinceType.valueOf(((String) p.get("type")).toUpperCase());
            String homeNationStr = (String) p.get("homeNation");
            Nation homeNation = homeNationStr != null ? new Nation(homeNationStr) : null;
            boolean supplyCenter = Boolean.TRUE.equals(p.get("supplyCenter"));

            Object coastsObj = p.get("coasts");
            List<Coast> coasts;
            if (coastsObj instanceof List<?> coastsList) {
                coasts = coastsList.stream().map(c -> new Coast((String) c)).toList();
            } else {
                coasts = List.of();
            }

            Object adjObj = p.get("adjacencies");
            Map<String, Coast> adjacencies = new HashMap<>();
            if (adjObj instanceof Map<?, ?> adjRaw) {
                for (var entry : adjRaw.entrySet()) {
                    adjacencies.put((String) entry.getKey(),
                            entry.getValue() != null ? new Coast((String) entry.getValue()) : null);
                }
            }

            provinces.add(new Province(name, type, homeNation, supplyCenter, coasts, adjacencies));
        }

        String id = mongoId != null ? mongoId : (String) root.get("id");
        String name = (String) root.get("name");
        return new GameMap(id != null ? id : "europe-classic",
                name != null ? name : "Classic Diplomacy Europe", provinces);
    }

    private List<Unit> parseInitialUnits(Map<String, Object> root) {
        if (!(root.get("initialUnits") instanceof List<?> initialUnitsRaw)) {
            return List.of();
        }

        List<Unit> units = new ArrayList<>();
        for (Object groupObj : initialUnitsRaw) {
            if (!(groupObj instanceof Map<?, ?> nationGroup)) continue;
            if (!(nationGroup.get("units") instanceof List<?> rawUnits)) continue;
            for (Object unitObj : rawUnits) {
                if (!(unitObj instanceof Map<?, ?> rawUnit)) continue;
                Nation nation = new Nation((String) rawUnit.get("nation"));
                UnitType unitType = UnitType.valueOf(((String) rawUnit.get("unitType")).toUpperCase());
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
