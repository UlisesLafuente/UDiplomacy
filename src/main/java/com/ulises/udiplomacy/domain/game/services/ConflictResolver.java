package com.ulises.udiplomacy.domain.game.services;

import com.ulises.udiplomacy.domain.game.*;
import com.ulises.udiplomacy.domain.game.enums.OrderResult;
import com.ulises.udiplomacy.domain.game.enums.OrderType;

import java.util.*;
import java.util.stream.Collectors;

public final class ConflictResolver {

    public ResolutionResult resolve(List<Order> orders, List<Unit> units, GameMap gameMap) {
        Map<String, Unit> unitByProvince = units.stream()
                .collect(Collectors.toMap(u -> u.location().provinceName(), u -> u));

        Map<Order, Integer> supportCounts = computeSupport(orders);
        Set<String> attackedProvinces = findAttackedProvinces(orders);
        Set<String> attackedUnits = findAttackedUnits(orders);
        Set<String> brokenSupports = findBrokenSupports(orders, attackedUnits, unitByProvince);

        Map<String, List<Order>> movesToProvince = groupMovesByTarget(orders);
        Set<String> contestedProvinces = findContestedProvinces(movesToProvince);

        List<Order> successfulMoves = resolveMoves(orders, movesToProvince, contestedProvinces,
                supportCounts, brokenSupports, unitByProvince);

        Set<String> movedFrom = successfulMoves.stream()
                .map(o -> o.source().provinceName())
                .collect(Collectors.toSet());

        Map<Unit, List<Territory>> dislodged = findDislodgedUnits(orders, successfulMoves,
                unitByProvince, gameMap, contestedProvinces, movedFrom);

        DislodgementResult dislodgementResult = new DislodgementResult(dislodged, contestedProvinces);

        Map<Order, OrderResult> orderResults = computeOrderResults(orders, successfulMoves,
                brokenSupports);

        return new ResolutionResult(dislodgementResult, orderResults);
    }

    private Map<Order, OrderResult> computeOrderResults(List<Order> orders,
                                                         List<Order> successfulMoves,
                                                         Set<String> brokenSupports) {
        Map<Order, OrderResult> results = new HashMap<>();
        for (Order order : orders) {
            results.put(order, switch (order.type()) {
                case HOLD -> OrderResult.SUCCESS;
                case MOVE -> successfulMoves.contains(order)
                        ? OrderResult.SUCCESS : OrderResult.FAILURE;
                case SUPPORT -> brokenSupports.contains(order.source().provinceName())
                        ? OrderResult.FAILURE : OrderResult.SUCCESS;
                case CONVOY -> OrderResult.SUCCESS;
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

    private Set<String> findAttackedUnits(List<Order> orders) {
        return orders.stream()
                .filter(o -> o.type() == OrderType.MOVE)
                .map(o -> o.target().map(Territory::provinceName).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private Set<String> findBrokenSupports(List<Order> orders, Set<String> attackedUnits,
                                            Map<String, Unit> unitByProvince) {
        Set<String> broken = new HashSet<>();
        for (Order order : orders) {
            if (order.type() == OrderType.SUPPORT) {
                String supporterProvince = order.source().provinceName();
                if (attackedUnits.contains(supporterProvince)) {
                    broken.add(supporterProvince);
                }
            }
        }
        return broken;
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

    private Set<String> findContestedProvinces(Map<String, List<Order>> movesToProvince) {
        Set<String> contested = new HashSet<>();
        for (var entry : movesToProvince.entrySet()) {
            if (entry.getValue().size() > 1) {
                contested.add(entry.getKey());
            }
        }
        return contested;
    }

    private List<Order> resolveMoves(List<Order> orders,
                                      Map<String, List<Order>> movesToProvince,
                                      Set<String> contestedProvinces,
                                      Map<Order, Integer> supportCounts,
                                      Set<String> brokenSupports,
                                      Map<String, Unit> unitByProvince) {
        List<Order> successful = new ArrayList<>();
        for (Order order : orders) {
            if (order.type() != OrderType.MOVE) continue;

            String target = order.target().map(Territory::provinceName).orElse(null);
            if (target == null) continue;

            if (contestedProvinces.contains(target)) {
                continue;
            }

            int support = supportCounts.getOrDefault(order, 0);
            if (brokenSupports.contains(order.source().provinceName())) {
                continue;
            }

            Unit defender = unitByProvince.get(target);
            if (defender != null) {
                Order defenseOrder = findOrderForUnit(orders, target);
                int defenseStrength = 1;
                if (defenseOrder != null
                        && (defenseOrder.type() == OrderType.HOLD
                        || defenseOrder.type() == OrderType.MOVE)) {
                    if (defenseOrder.type() == OrderType.HOLD) {
                        defenseStrength = 1 + supportCounts.getOrDefault(defenseOrder, 0);
                    }
                }

                int attackStrength = 1 + support;
                if (attackStrength > defenseStrength) {
                    successful.add(order);
                }
            } else {
                if (support >= 0) {
                    successful.add(order);
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
}
