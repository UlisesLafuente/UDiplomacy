package com.ulises.udiplomacy.infrastructure.web.dto.response;

import com.ulises.udiplomacy.domain.game.*;
import com.ulises.udiplomacy.domain.game.enums.OrderResult;

import java.util.*;
import java.util.stream.Collectors;

public record GameResponse(
        String gameId,
        String mapId,
        String mapName,
        String state,
        String season,
        int year,
        String phase,
        List<UnitResponse> units,
        List<String> nations,
        List<OrderResponse> pendingOrders,
        List<OrderResponse> lastResolvedOrders,
        List<String> lastResolvedResults,
        List<HistoryEntry> history,
        List<UnitResponse> dislodgedUnits,
        List<BuildCapacityResponse> buildCapacities,
        Map<String, String> provinceOwnership,
        Map<String, Integer> scores,
        Map<String, String> provinceTypes,
        boolean colonialRule
) {
    public static GameResponse from(Game game) {
        var lastResolved = game.turnHistory().isEmpty()
                ? game.currentTurn().resolvedOrders()
                : game.turnHistory().getLast().resolvedOrders();
        var lastResults = game.turnHistory().isEmpty()
                ? game.currentTurn().resolvedResults()
                : game.turnHistory().getLast().resolvedResults();

        var history = game.turnHistory().stream()
                .map(t -> new HistoryEntry(
                        t.season().name(), t.year(), t.phase().name(),
                        t.units().stream().map(UnitResponse::from).toList(),
                        t.resolvedOrders().stream().map(OrderResponse::from).toList(),
                        t.resolvedOrders().stream()
                                .map(o -> t.resolvedResults().getOrDefault(o, OrderResult.SUCCESS).name())
                                .toList()
                ))
                .toList();

        var dislodged = game.dislodgementResult()
                .map(dr -> dr.dislodgedUnits().keySet().stream()
                        .map(UnitResponse::from)
                        .toList())
                .orElse(List.of());

        var buildCapacities = game.nations().stream()
                .map(n -> {
                    var opts = game.getBuildOptions(n);
                    var homeProvinces = game.gameMap().homeCentersFor(n).stream()
                            .map(Province::name)
                            .filter(p -> game.currentTurn().units().stream()
                                    .noneMatch(u -> u.location().provinceName().equals(p)))
                            .sorted()
                            .toList();
                    var homeCenterNames = game.gameMap().homeCentersFor(n).stream()
                            .map(Province::name)
                            .collect(java.util.stream.Collectors.toSet());
                    var colonialProvinces = game.gameMap().supplyCenters().stream()
                            .map(Province::name)
                            .filter(p -> game.provinceOwnership().getOrDefault(p, null) != null
                                    && game.provinceOwnership().get(p).equals(n))
                            .filter(p -> !homeCenterNames.contains(p))
                            .filter(p -> game.currentTurn().units().stream()
                                    .noneMatch(u -> u.location().provinceName().equals(p)))
                            .sorted()
                            .toList();
                    return new BuildCapacityResponse(
                            n.name(),
                            opts.buildsAllowed(),
                            opts.disbandsRequired(),
                            homeProvinces,
                            opts.colonialBuildsAvailable(),
                            colonialProvinces
                    );
                })
                .toList();

        var ownership = new HashMap<String, String>();
        for (var entry : game.provinceOwnership().entrySet()) {
            ownership.put(entry.getKey(), entry.getValue().name());
        }

        var scores = new HashMap<String, Integer>();
        for (var entry : game.finalScores().entrySet()) {
            scores.put(entry.getKey().name(), entry.getValue());
        }

        var provinceTypes = new HashMap<String, String>();
        for (var entry : game.gameMap().provinces().entrySet()) {
            provinceTypes.put(entry.getKey(), entry.getValue().type().name());
        }

        return new GameResponse(
                game.gameId(),
                game.gameMap().id(),
                game.gameMap().name(),
                game.state().name(),
                game.currentTurn().season().name(),
                game.currentTurn().year(),
                game.currentTurn().phase().name(),
                game.currentTurn().units().stream().map(UnitResponse::from).toList(),
                game.nations().stream().map(Nation::name).toList(),
                game.orderPool().orders().stream().map(OrderResponse::from).toList(),
                lastResolved.stream().map(OrderResponse::from).toList(),
                lastResolved.stream()
                        .map(o -> lastResults.getOrDefault(o, OrderResult.SUCCESS).name())
                        .toList(),
                history,
                dislodged,
                buildCapacities,
                ownership,
                scores,
                provinceTypes,
                game.colonialRule()
        );
    }

    public record HistoryEntry(String season, int year, String phase,
                                List<UnitResponse> units,
                                List<OrderResponse> orders,
                                List<String> results) {}

    public record UnitResponse(String type, String nation, String province) {
        static UnitResponse from(Unit u) {
            return new UnitResponse(u.unitType().name(), u.nation().name(), u.location().provinceName());
        }
    }

    public record OrderResponse(String type, String unitType, String nation,
                                 String source, String target, String auxiliary) {
        static OrderResponse from(Order o) {
            return new OrderResponse(
                    o.type().name(),
                    o.unit().unitType().name(),
                    o.unit().nation() != null ? o.unit().nation().name() : null,
                    o.source().provinceName(),
                    o.target().map(Territory::provinceName).orElse(null),
                    o.auxiliary().map(Territory::provinceName).orElse(null)
            );
        }
    }

    public record BuildCapacityResponse(String nation, int buildsAvailable, int disbandsRequired,
                                         List<String> availableProvinces, int colonialBuildsAvailable,
                                         List<String> colonialProvinces) {}
}
