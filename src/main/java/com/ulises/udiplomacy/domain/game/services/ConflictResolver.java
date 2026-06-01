package com.ulises.udiplomacy.domain.game.services;

import com.ulises.udiplomacy.domain.game.*;
import com.ulises.udiplomacy.domain.game.enums.OrderResult;
import com.ulises.udiplomacy.domain.game.enums.OrderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public final class ConflictResolver {
    private static final Logger log = LoggerFactory.getLogger(ConflictResolver.class);

    public ResolutionResult resolve(List<Order> orders, List<Unit> units, GameMap gameMap) {
        Map<String, Unit> unitByProvince = units.stream()
                .collect(Collectors.toMap(u -> u.location().provinceName(), u -> u));

        Map<Order, Integer> supportCounts = computeSupport(orders);
        Set<String> attackedProvinces = findAttackedProvinces(orders);
        Set<String> brokenSupports = findBrokenSupports(orders, attackedProvinces);
        Map<Order, Integer> effectiveSupport = adjustBrokenSupports(orders, supportCounts, brokenSupports);

        Set<String> invalidMoves = findInvalidMoves(orders, gameMap);
        Map<String, List<Order>> movesToProvince = groupMovesByTarget(orders);
        Set<String> contestedProvinces = findContestedProvinces(movesToProvince, invalidMoves);

        List<Order> successfulMoves = resolveMoves(orders, movesToProvince, contestedProvinces,
                effectiveSupport, unitByProvince, invalidMoves);

        Set<String> movedFrom = successfulMoves.stream()
                .map(o -> o.source().provinceName())
                .collect(Collectors.toSet());

        Map<Unit, List<Territory>> dislodged = findDislodgedUnits(orders, successfulMoves,
                unitByProvince, gameMap, contestedProvinces, movedFrom);

        DislodgementResult dislodgementResult = new DislodgementResult(dislodged, contestedProvinces);

        Map<Order, OrderResult> orderResults = computeOrderResults(orders, successfulMoves,
                attackedProvinces);

        // Log summary
        if (!contestedProvinces.isEmpty()) {
            log.info("Contested provinces: {}", contestedProvinces);
        }
        if (!brokenSupports.isEmpty()) {
            log.info("Broken supports from: {}", brokenSupports);
        }
        if (!successfulMoves.isEmpty()) {
            log.info("Successful moves: {}",
                    successfulMoves.stream()
                            .map(o -> o.source().provinceName() + " -> " + o.target().map(Territory::provinceName).orElse("?"))
                            .collect(Collectors.joining(", ")));
        }
        for (var entry : dislodged.entrySet()) {
            Unit u = entry.getKey();
            List<Territory> options = entry.getValue();
            log.info("Dislodged: {} {} in {} | Retreat options: {}",
                    u.nation(), u.unitType(), u.location().provinceName(),
                    options.isEmpty() ? "none (must disband)"
                            : options.stream().map(Territory::provinceName).collect(Collectors.joining(", ")));
        }

        return new ResolutionResult(dislodgementResult, orderResults);
    }

    private Map<Order, OrderResult> computeOrderResults(List<Order> orders,
                                                          List<Order> successfulMoves,
                                                          Set<String> attackedProvinces) {
        Map<Order, OrderResult> results = new HashMap<>();
        for (Order order : orders) {
            results.put(order, switch (order.type()) {
                case HOLD -> OrderResult.SUCCESS;
                case MOVE -> successfulMoves.contains(order)
                        ? OrderResult.SUCCESS : OrderResult.FAILURE;
                case SUPPORT -> attackedProvinces.contains(order.source().provinceName())
                        ? OrderResult.FAILURE : OrderResult.SUCCESS;
                case CONVOY -> attackedProvinces.contains(order.source().provinceName())
                        ? OrderResult.FAILURE : OrderResult.SUCCESS;
                case RETREAT, BUILD, DISBAND -> OrderResult.SUCCESS;
            });
        }
        return results;
    }

    private Map<Order, Integer> computeSupport(List<Order> orders) {
        Map<Order, Integer> counts = new HashMap<>();
        for (Order order : orders) {
            if (order.type() == OrderType.SUPPORT) {
                Order targetOrder = findOrderForUnit(orders, order.auxiliary()
                        .map(Territory::provinceName).orElse(null));
                if (targetOrder != null) {
                    counts.merge(targetOrder, 1, Integer::sum);
                }
            }
        }
        return counts;
    }

    private Set<String> findAttackedProvinces(List<Order> orders) {
        return orders.stream()
                .filter(o -> o.type() == OrderType.MOVE)
                .map(o -> o.target().map(Territory::provinceName).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private Set<String> findBrokenSupports(List<Order> orders, Set<String> attackedProvinces) {
        Set<String> broken = new HashSet<>();
        for (Order order : orders) {
            if (order.type() == OrderType.SUPPORT) {
                String supporterProvince = order.source().provinceName();
                if (attackedProvinces.contains(supporterProvince)) {
                    broken.add(supporterProvince);
                }
            }
        }
        return broken;
    }

    private Map<Order, Integer> adjustBrokenSupports(List<Order> orders,
                                                      Map<Order, Integer> supportCounts,
                                                      Set<String> brokenSupports) {
        Map<Order, Integer> adjusted = new HashMap<>(supportCounts);
        for (Order order : orders) {
            if (order.type() == OrderType.SUPPORT
                    && brokenSupports.contains(order.source().provinceName())) {
                Order targetOrder = findOrderForUnit(orders, order.auxiliary()
                        .map(Territory::provinceName).orElse(null));
                if (targetOrder != null) {
                    adjusted.merge(targetOrder, -1, Integer::sum);
                }
            }
        }
        return adjusted;
    }

    private Map<String, List<Order>> groupMovesByTarget(List<Order> orders) {
        Map<String, List<Order>> moves = new HashMap<>();
        for (Order order : orders) {
            if (order.type() == OrderType.MOVE) {
                order.target().ifPresent(target -> {
                    moves.computeIfAbsent(target.provinceName(), k -> new ArrayList<>()).add(order);
                });
            }
        }
        return moves;
    }

    private Set<String> findInvalidMoves(List<Order> orders, GameMap gameMap) {
        Set<String> invalidSources = new HashSet<>();
        for (Order order : orders) {
            if (order.type() != OrderType.MOVE) continue;
            String source = order.source().provinceName();
            String target = order.target().map(Territory::provinceName).orElse(null);
            if (target == null) {
                invalidSources.add(source);
                continue;
            }
            Province srcProv = gameMap.province(source).orElse(null);
            Province tgtProv = gameMap.province(target).orElse(null);
            if (srcProv == null || tgtProv == null) {
                invalidSources.add(source);
                continue;
            }
            // Not adjacent — check for convoy path for armies
            if (!srcProv.adjacencies().containsKey(target)
                    && !tgtProv.adjacencies().containsKey(source)) {
                if (order.unit().unitType() == com.ulises.udiplomacy.domain.game.enums.UnitType.ARMY
                        && hasConvoyPath(order, orders, gameMap)) {
                    // Valid convoy path exists
                } else {
                    invalidSources.add(source);
                    continue;
                }
            }
            // Fleet must move via coastal adjacency (non-null coast)
            if (order.unit().unitType() == com.ulises.udiplomacy.domain.game.enums.UnitType.FLEET) {
                Coast srcCoast = srcProv.adjacencies().get(target);
                Coast tgtCoast = tgtProv.adjacencies().get(source);
                if (srcCoast == null && tgtCoast == null) {
                    if (srcProv.isSea() || tgtProv.isSea()) {
                        // Sea <-> Coast or Sea <-> Sea: always a valid sea route
                    } else if (!srcProv.coasts().isEmpty() || !tgtProv.coasts().isEmpty()) {
                        // At least one province has named coasts, null means land-only
                        invalidSources.add(source);
                        continue;
                    } else if (!shareSeaNeighbor(srcProv, tgtProv, gameMap)) {
                        // Single-coast provinces with no shared sea = land-only route
                        invalidSources.add(source);
                        continue;
                    }
                }
            }
            // Army to sea
            if (order.unit().unitType() == com.ulises.udiplomacy.domain.game.enums.UnitType.ARMY
                    && tgtProv.isSea()) {
                invalidSources.add(source);
                continue;
            }
            // Fleet to inland
            if (order.unit().unitType() == com.ulises.udiplomacy.domain.game.enums.UnitType.FLEET
                    && tgtProv.isInland()) {
                invalidSources.add(source);
                continue;
            }
        }
        return invalidSources;
    }

    private Set<String> findContestedProvinces(Map<String, List<Order>> movesToProvince,
                                                  Set<String> invalidMoves) {
        Set<String> contested = new HashSet<>();
        for (var entry : movesToProvince.entrySet()) {
            long validCount = entry.getValue().stream()
                    .filter(o -> !invalidMoves.contains(o.source().provinceName()))
                    .count();
            if (validCount > 1) {
                contested.add(entry.getKey());
            }
        }
        return contested;
    }

    private List<Order> resolveMoves(List<Order> orders,
                                        Map<String, List<Order>> movesToProvince,
                                        Set<String> contestedProvinces,
                                        Map<Order, Integer> effectiveSupport,
                                        Map<String, Unit> unitByProvince,
                                        Set<String> invalidMoves) {
        List<Order> successful = new ArrayList<>();
        for (Order order : orders) {
            if (order.type() != OrderType.MOVE) continue;

            String target = order.target().map(Territory::provinceName).orElse(null);
            if (target == null) continue;

            if (invalidMoves.contains(order.source().provinceName())) {
                log.debug("  MOVE {} -> {}: FAIL (invalid move – not adjacent or wrong terrain)",
                        order.source().provinceName(), target);
                continue;
            }

            if (contestedProvinces.contains(target)) {
                log.debug("  MOVE {} -> {}: FAIL (contested)",
                        order.source().provinceName(), target);
                continue;
            }

            int support = effectiveSupport.getOrDefault(order, 0);

            Unit defender = unitByProvince.get(target);
            if (defender != null) {
                Order defenseOrder = findOrderForUnit(orders, target);
                int defenseStrength = 1;
                if (defenseOrder != null
                        && (defenseOrder.type() == OrderType.HOLD
                        || defenseOrder.type() == OrderType.MOVE)) {
                    if (defenseOrder.type() == OrderType.HOLD) {
                        defenseStrength = 1 + effectiveSupport.getOrDefault(defenseOrder, 0);
                    } else {
                        defenseStrength = 0;
                    }
                }

                int attackStrength = 1 + support;
                if (attackStrength > defenseStrength) {
                    successful.add(order);
                    log.debug("  MOVE {} -> {}: SUCCESS ({} > {})",
                            order.source().provinceName(), target, attackStrength, defenseStrength);
                } else {
                    log.debug("  MOVE {} -> {}: FAIL ({} <= {})",
                            order.source().provinceName(), target, attackStrength, defenseStrength);
                }
            } else {
                if (support >= 0) {
                    successful.add(order);
                    log.debug("  MOVE {} -> {}: SUCCESS (empty province)",
                            order.source().provinceName(), target);
                }
            }
        }
        return successful;
    }

    private Map<Unit, List<Territory>> findDislodgedUnits(List<Order> orders,
                                                           List<Order> successfulMoves,
                                                           Map<String, Unit> unitByProvince,
                                                           GameMap gameMap,
                                                           Set<String> contestedProvinces,
                                                           Set<String> movedFrom) {
        Map<Unit, List<Territory>> dislodged = new HashMap<>();
        Set<String> dislodgedProvinces = successfulMoves.stream()
                .map(o -> o.target().map(Territory::provinceName).orElse(null))
                .filter(Objects::nonNull)
                .filter(target -> unitByProvince.containsKey(target)
                        && !movedFrom.contains(target))
                .collect(Collectors.toSet());

        for (String provinceName : dislodgedProvinces) {
            Unit dislodgedUnit = unitByProvince.get(provinceName);
            if (dislodgedUnit != null) {
                List<Territory> retreatOptions = computeRetreatOptions(dislodgedUnit,
                        orders, gameMap, contestedProvinces, movedFrom);
                dislodged.put(dislodgedUnit, retreatOptions);
            }
        }
        return dislodged;
    }

    private List<Territory> computeRetreatOptions(Unit unit, List<Order> orders,
                                                    GameMap gameMap,
                                                    Set<String> contestedProvinces,
                                                    Set<String> movedFrom) {
        Province currentProvince = gameMap.province(unit.location().provinceName()).orElse(null);
        if (currentProvince == null) return List.of();

        List<Territory> options = new ArrayList<>();
        for (var entry : currentProvince.adjacencies().entrySet()) {
            String neighborName = entry.getKey();
            if (contestedProvinces.contains(neighborName)) continue;
            if (movedFrom.contains(neighborName)) continue;
            if (unitAtProvince(orders, neighborName).isPresent()) continue;

            Province neighbor = gameMap.province(neighborName).orElse(null);
            if (neighbor == null) continue;

            if (unit.unitType() == com.ulises.udiplomacy.domain.game.enums.UnitType.ARMY
                    && neighbor.isSea()) continue;
            if (unit.unitType() == com.ulises.udiplomacy.domain.game.enums.UnitType.FLEET
                    && neighbor.isInland()) continue;

            Coast coast = entry.getValue();
            options.add(coast != null ? new Territory(neighborName, coast) : new Territory(neighborName));
        }
        return options;
    }

    private Order findOrderForUnit(List<Order> orders, String provinceName) {
        return orders.stream()
                .filter(o -> o.source().provinceName().equals(provinceName))
                .findFirst()
                .orElse(null);
    }

    private Optional<Unit> unitAtProvince(List<Order> orders, String provinceName) {
        return orders.stream()
                .map(Order::unit)
                .filter(u -> u.location().provinceName().equals(provinceName))
                .findFirst();
    }

    private boolean hasConvoyPath(Order armyMove, List<Order> orders, GameMap gameMap) {
        String source = armyMove.source().provinceName();
        String target = armyMove.target().map(Territory::provinceName).orElse(null);
        if (target == null) return false;

        // Find matching CONVOY orders: fleet in SEA, with matching auxiliary=source and target=target
        Set<String> convoySeaProvinces = orders.stream()
                .filter(o -> o.type() == OrderType.CONVOY)
                .filter(o -> o.auxiliary().map(Territory::provinceName).orElse("").equals(source))
                .filter(o -> o.target().map(Territory::provinceName).orElse("").equals(target))
                .map(o -> o.source().provinceName())
                .filter(prov -> {
                    Province p = gameMap.province(prov).orElse(null);
                    return p != null && p.isSea();
                })
                .collect(Collectors.toSet());

        if (convoySeaProvinces.isEmpty()) return false;

        Province srcProv = gameMap.province(source).orElse(null);
        Province tgtProv = gameMap.province(target).orElse(null);
        if (srcProv == null || tgtProv == null) return false;

        // Sea provinces adjacent to source
        Set<String> srcSeaAdj = srcProv.adjacencies().keySet().stream()
                .filter(n -> {
                    Province p = gameMap.province(n).orElse(null);
                    return p != null && p.isSea();
                })
                .collect(Collectors.toSet());

        // Sea provinces adjacent to target
        Set<String> tgtSeaAdj = tgtProv.adjacencies().keySet().stream()
                .filter(n -> {
                    Province p = gameMap.province(n).orElse(null);
                    return p != null && p.isSea();
                })
                .collect(Collectors.toSet());

        if (srcSeaAdj.isEmpty() || tgtSeaAdj.isEmpty()) return false;

        // BFS through sea provinces with matching CONVOY orders
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new ArrayDeque<>();
        for (String sea : srcSeaAdj) {
            if (convoySeaProvinces.contains(sea)) {
                queue.add(sea);
            }
        }

        while (!queue.isEmpty()) {
            String current = queue.poll();
            if (!visited.add(current)) continue;

            if (tgtSeaAdj.contains(current)) return true;

            Province currentProv = gameMap.province(current).orElse(null);
            if (currentProv == null) continue;

            for (String neighbor : currentProv.adjacencies().keySet()) {
                if (visited.contains(neighbor)) continue;
                Province neighborProv = gameMap.province(neighbor).orElse(null);
                if (neighborProv != null && neighborProv.isSea()
                        && convoySeaProvinces.contains(neighbor)) {
                    queue.add(neighbor);
                }
            }
        }

        return false;
    }

    private boolean shareSeaNeighbor(Province a, Province b, GameMap gameMap) {
        for (String neighborName : a.adjacencies().keySet()) {
            if (b.adjacencies().containsKey(neighborName)) {
                Province neighbor = gameMap.province(neighborName).orElse(null);
                if (neighbor != null && neighbor.isSea()) {
                    return true;
                }
            }
        }
        return false;
    }
}
