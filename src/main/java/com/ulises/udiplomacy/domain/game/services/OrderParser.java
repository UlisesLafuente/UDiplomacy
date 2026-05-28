package com.ulises.udiplomacy.domain.game.services;

import com.ulises.udiplomacy.domain.game.*;
import com.ulises.udiplomacy.domain.game.enums.OrderType;
import com.ulises.udiplomacy.domain.game.enums.UnitType;

import java.util.Optional;
import java.util.regex.Pattern;

public final class OrderParser {
    private static final Pattern ORDER_PATTERN = Pattern.compile(
            "^(ARMY|FLEET)\\s+(\\w+)\\s+([\\-SCRBD])\\s*(\\w+)?(?:\\s+(\\w+))?$"
    );

    public Order parse(String raw, GameMap gameMap) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Order cannot be empty");
        }

        var parts = raw.trim().split("\\s+");
        if (parts.length < 3) {
            throw new IllegalArgumentException("Invalid order format: " + raw);
        }

        UnitType unitType = parseUnitType(parts[0]);
        String sourceProvince = parts[1];
        String action = parts[2];

        Province source = gameMap.province(sourceProvince)
                .orElseThrow(() -> new IllegalArgumentException("Unknown province: " + sourceProvince));

        Unit stubUnit = new Unit(unitType, null, new Territory(sourceProvince));
        Territory sourceTerritory = new Territory(sourceProvince);

        return switch (action) {
            case "H" -> {
                if (!validateUnitInProvince(unitType, source)) {
                    throw new IllegalArgumentException(unitType + " cannot be in " + sourceProvince);
                }
                yield new Order(OrderType.HOLD, stubUnit, sourceTerritory, null, null);
            }
            case "-" -> {
                String targetProvince = parts.length > 3 ? parts[3] : null;
                if (targetProvince == null) {
                    throw new IllegalArgumentException("Move order requires a target province");
                }
                Territory target = new Territory(targetProvince);
                validateMove(unitType, sourceProvince, targetProvince, gameMap);
                yield new Order(OrderType.MOVE, stubUnit, sourceTerritory, target, null);
            }
            case "S" -> {
                String supportedUnit = parts.length > 3 ? parts[3] : null;
                String targetOfSupport = parts.length > 4 ? parts[4] : null;
                if (supportedUnit == null) {
                    throw new IllegalArgumentException("Support order requires a target unit");
                }
                Territory aux = new Territory(supportedUnit);
                Territory target = targetOfSupport != null ? new Territory(targetOfSupport) : null;
                yield new Order(OrderType.SUPPORT, stubUnit, sourceTerritory, target, aux);
            }
            case "C" -> {
                String transportedUnit = parts.length > 3 ? parts[3] : null;
                String targetOfConvoy = parts.length > 4 ? parts[4] : null;
                if (transportedUnit == null) {
                    throw new IllegalArgumentException("Convoy order requires a unit to convoy");
                }
                Territory aux = new Territory(transportedUnit);
                Territory target = targetOfConvoy != null ? new Territory(targetOfConvoy) : null;
                yield new Order(OrderType.CONVOY, stubUnit, sourceTerritory, target, aux);
            }
            case "R" -> {
                String retreatTarget = parts.length > 3 ? parts[3] : null;
                if (retreatTarget == null) {
                    throw new IllegalArgumentException("Retreat order requires a target province");
                }
                yield new Order(OrderType.RETREAT, stubUnit, sourceTerritory,
                        new Territory(retreatTarget), null);
            }
            case "B" -> {
                String buildTarget = parts.length > 3 ? parts[3] : null;
                if (buildTarget == null) {
                    throw new IllegalArgumentException("Build order requires a target province");
                }
                yield new Order(OrderType.BUILD, stubUnit, sourceTerritory,
                        new Territory(buildTarget), null);
            }
            case "D" -> {
                yield new Order(OrderType.DISBAND, stubUnit, sourceTerritory, null, null);
            }
            default -> throw new IllegalArgumentException("Unknown action: " + action);
        };
    }

    private UnitType parseUnitType(String s) {
        return switch (s.toUpperCase()) {
            case "ARMY", "A" -> UnitType.ARMY;
            case "FLEET", "F" -> UnitType.FLEET;
            default -> throw new IllegalArgumentException("Unknown unit type: " + s);
        };
    }

    private boolean validateUnitInProvince(UnitType unitType, Province province) {
        return switch (unitType) {
            case ARMY -> province.type() != com.ulises.udiplomacy.domain.game.enums.ProvinceType.SEA;
            case FLEET -> province.isCoastal() || province.isSea();
        };
    }

    private void validateMove(UnitType unitType, String source, String target, GameMap gameMap) {
        Province srcProvince = gameMap.province(source)
                .orElseThrow(() -> new IllegalArgumentException("Unknown source: " + source));
        Province tgtProvince = gameMap.province(target)
                .orElseThrow(() -> new IllegalArgumentException("Unknown target: " + target));

        if (!validateUnitInProvince(unitType, srcProvince)) {
            throw new IllegalArgumentException(unitType + " cannot be in " + source);
        }

        if (unitType == UnitType.ARMY && tgtProvince.isSea()) {
            throw new IllegalArgumentException("Army cannot move to sea province: " + target);
        }
        if (unitType == UnitType.FLEET && tgtProvince.isInland()) {
            throw new IllegalArgumentException("Fleet cannot move to inland province: " + target);
        }

        if (!srcProvince.isAdjacentTo(target)) {
            throw new IllegalArgumentException(source + " is not adjacent to " + target);
        }
    }
}
