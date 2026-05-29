package com.ulises.udiplomacy.domain.game.services;

import com.ulises.udiplomacy.domain.game.*;
import com.ulises.udiplomacy.domain.game.enums.OrderType;
import com.ulises.udiplomacy.domain.game.enums.UnitType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.regex.Pattern;

public final class OrderParser {
    private static final Logger log = LoggerFactory.getLogger(OrderParser.class);
    private static final Pattern ORDER_PATTERN = Pattern.compile(
            "^(ARMY|FLEET)\\s+(\\w+)\\s+([\\-SCRBD])\\s*(\\w+)?(?:\\s+(\\w+))?$"
    );

    public Order parse(String raw, GameMap gameMap) {
        if (raw == null || raw.isBlank()) {
            log.warn("Parse failed: empty order");
            throw new IllegalArgumentException("Order cannot be empty");
        }

        var parts = raw.trim().split("\\s+");
        if (parts.length < 3) {
            log.warn("Parse failed: too few tokens in '{}'", raw);
            throw new IllegalArgumentException("Invalid order format: " + raw);
        }

        UnitType unitType = parseUnitType(parts[0]);
        String sourceProvince = parts[1];
        String action = parts[2];

        Province source = gameMap.province(sourceProvince)
                .orElseThrow(() -> {
                    log.warn("Parse failed: unknown province '{}' in '{}'", sourceProvince, raw);
                    return new IllegalArgumentException("Unknown province: " + sourceProvince);
                });

        Unit stubUnit = new Unit(unitType, null, new Territory(sourceProvince));
        Territory sourceTerritory = new Territory(sourceProvince);

        return switch (action) {
            case "H" -> {
                Order o = new Order(OrderType.HOLD, stubUnit, sourceTerritory, null, null);
                log.debug("Parsed: {} -> HOLD", raw);
                yield o;
            }
            case "-" -> {
                String targetProvince = parts.length > 3 ? parts[3] : null;
                if (targetProvince == null) {
                    log.warn("Parse failed: move without target '{}'", raw);
                    throw new IllegalArgumentException("Move order requires a target province");
                }
                Order o = new Order(OrderType.MOVE, stubUnit, sourceTerritory,
                        new Territory(targetProvince), null);
                log.debug("Parsed: {} -> MOVE {} -> {}", raw, sourceProvince, targetProvince);
                yield o;
            }
            case "S" -> {
                int idx = parts.length > 3 ? skipUnitPrefix(parts, 3) : 0;
                if (idx == 0) {
                    throw new IllegalArgumentException("Support order requires a target unit");
                }
                String supportedUnit = parts[idx];
                String targetOfSupport = parts.length > idx + 1 ? parts[idx + 1] : null;
                if (targetOfSupport != null && targetOfSupport.equals("-")) {
                    targetOfSupport = parts.length > idx + 2 ? parts[idx + 2] : null;
                }
                Territory aux = new Territory(supportedUnit);
                Territory target = targetOfSupport != null ? new Territory(targetOfSupport) : null;
                Order o = new Order(OrderType.SUPPORT, stubUnit, sourceTerritory, target, aux);
                log.debug("Parsed: {} -> SUPPORT {} -> {}", raw, supportedUnit,
                        targetOfSupport != null ? targetOfSupport : "(hold)");
                yield o;
            }
            case "C" -> {
                int idx = parts.length > 3 ? skipUnitPrefix(parts, 3) : 0;
                if (idx == 0) {
                    throw new IllegalArgumentException("Convoy order requires a unit to convoy");
                }
                String transportedUnit = parts[idx];
                String targetOfConvoy = parts.length > idx + 1 ? parts[idx + 1] : null;
                if (targetOfConvoy != null && targetOfConvoy.equals("-")) {
                    targetOfConvoy = parts.length > idx + 2 ? parts[idx + 2] : null;
                }
                Territory aux = new Territory(transportedUnit);
                Territory target = targetOfConvoy != null ? new Territory(targetOfConvoy) : null;
                Order o = new Order(OrderType.CONVOY, stubUnit, sourceTerritory, target, aux);
                log.debug("Parsed: {} -> CONVOY {} -> {}", raw, transportedUnit,
                        targetOfConvoy != null ? targetOfConvoy : "(unknown)");
                yield o;
            }
            case "R" -> {
                String retreatTarget = parts.length > 3 ? parts[3] : null;
                if (retreatTarget == null) {
                    throw new IllegalArgumentException("Retreat order requires a target province");
                }
                Order o = new Order(OrderType.RETREAT, stubUnit, sourceTerritory,
                        new Territory(retreatTarget), null);
                log.debug("Parsed: {} -> RETREAT {} -> {}", raw, sourceProvince, retreatTarget);
                yield o;
            }
            case "B" -> {
                String buildTarget = parts.length > 3 ? parts[3] : null;
                if (buildTarget == null) {
                    throw new IllegalArgumentException("Build order requires a target province");
                }
                Order o = new Order(OrderType.BUILD, stubUnit, sourceTerritory,
                        new Territory(buildTarget), null);
                log.debug("Parsed: {} -> BUILD {} in {}", raw, unitType, buildTarget);
                yield o;
            }
            case "D" -> {
                Order o = new Order(OrderType.DISBAND, stubUnit, sourceTerritory, null, null);
                log.debug("Parsed: {} -> DISBAND {}", raw, sourceProvince);
                yield o;
            }
            default -> {
                log.warn("Parse failed: unknown action '{}' in '{}'", action, raw);
                throw new IllegalArgumentException("Unknown action: " + action);
            }
        };
    }

    private int skipUnitPrefix(String[] parts, int start) {
        if (start >= parts.length) return 0;
        String val = parts[start];
        if (val.equals("A") || val.equals("F")
                || val.equals("ARMY") || val.equals("FLEET")) {
            return start + 1 < parts.length ? start + 1 : 0;
        }
        return start;
    }

    private UnitType parseUnitType(String s) {
        return switch (s.toUpperCase()) {
            case "ARMY", "A" -> UnitType.ARMY;
            case "FLEET", "F" -> UnitType.FLEET;
            default -> throw new IllegalArgumentException("Unknown unit type: " + s);
        };
    }
}
