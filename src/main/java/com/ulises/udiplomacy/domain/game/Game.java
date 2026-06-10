package com.ulises.udiplomacy.domain.game;

import com.ulises.udiplomacy.domain.game.enums.GameState;
import com.ulises.udiplomacy.domain.game.enums.Phase;
import com.ulises.udiplomacy.domain.game.enums.Season;
import com.ulises.udiplomacy.domain.game.enums.UnitType;
import com.ulises.udiplomacy.domain.game.enums.OrderResult;
import com.ulises.udiplomacy.domain.game.enums.OrderType;
import com.ulises.udiplomacy.domain.game.events.DomainEvent;
import com.ulises.udiplomacy.domain.game.events.GameFinished;
import com.ulises.udiplomacy.domain.game.events.GameStarted;
import com.ulises.udiplomacy.domain.game.events.PhaseEnded;
import com.ulises.udiplomacy.domain.game.events.TurnEnded;
import com.ulises.udiplomacy.domain.game.services.ConflictResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public final class Game {
    private static final Logger log = LoggerFactory.getLogger(Game.class);
    private static final int WIN_CONDITION_SCS = 18;

    private final String gameId;
    private final GameMap gameMap;
    private GameState state;
    private Turn currentTurn;
    private final List<Turn> turnHistory;
    private final Set<Nation> nations;
    private OrderPool orderPool;
    private DislodgementResult dislodgementResult;
    private Nation winner;
    private final List<DomainEvent> events;
    private final Map<String, Nation> provinceOwnership;
    private final boolean colonialRule;
    private Map<Nation, Integer> unusedBuilds;

    public Game(String gameId, GameMap gameMap, Set<Nation> nations) {
        this(gameId, gameMap, nations, false);
    }

    public Game(String gameId, GameMap gameMap, Set<Nation> nations, boolean colonialRule) {
        this.gameId = gameId;
        this.gameMap = gameMap;
        this.nations = new HashSet<>(nations);
        this.state = GameState.WAITING;
        this.orderPool = new OrderPool();
        this.turnHistory = new ArrayList<>();
        this.events = new ArrayList<>();
        this.currentTurn = new Turn(Season.SPRING, 1901, Phase.ORDERS, List.of());
        this.provinceOwnership = new HashMap<>();
        this.colonialRule = colonialRule;
        this.unusedBuilds = new HashMap<>();
    }

    public void start(List<Unit> initialUnits) {
        if (state != GameState.WAITING) {
            throw new IllegalStateException("Game already started");
        }
        state = GameState.IN_PROGRESS;
        currentTurn = new Turn(Season.SPRING, 1901, Phase.ORDERS, initialUnits);
        provinceOwnership.clear();
        for (Unit u : initialUnits) {
            provinceOwnership.put(u.location().provinceName(), u.nation());
        }
        events.add(new GameStarted(gameId, new HashSet<>(nations)));
        log.info("Game {} started. Spring 1901. Nations: {}. Units: {}",
                gameId, nations, initialUnits.size());
    }

    public void submitOrder(Order order) {
        if (state != GameState.IN_PROGRESS) {
            throw new IllegalStateException("Game is not in progress");
        }
        orderPool.add(order);
        log.debug("Game {} | Order submitted: {} {} -> {} aux={}",
                gameId, order.unit().unitType(), order.source().provinceName(),
                order.target().map(Territory::provinceName).orElse("-"),
                order.auxiliary().map(Territory::provinceName).orElse("-"));
    }

    public ResolutionResult executeOrders(ConflictResolver resolver) {
        if (state != GameState.IN_PROGRESS) {
            throw new IllegalStateException("Game is not in progress");
        }
        if (currentTurn.phase() != Phase.ORDERS) {
            throw new IllegalStateException("Can only execute orders in ORDERS phase");
        }

        autoFillHolds();
        log.info("Game {} | Resolving {} orders ({} submitted + auto-fill) | {} {} {}",
                gameId, orderPool.orders().size(),
                orderPool.orders().stream().filter(o -> o.type() != OrderType.HOLD).count(),
                currentTurn.season(), currentTurn.year(), currentTurn.phase());
        ResolutionResult resolution = resolver.resolve(orderPool.orders(), currentTurn.units(), gameMap);
        this.dislodgementResult = resolution.dislodgementResult();

        currentTurn = currentTurn.withOrdersResolved(orderPool.orders(), resolution.orderResults());

        // Match stub units (from OrderParser, nation=null) to actual game units
        Map<String, Unit> actualUnitsByProvince = new HashMap<>();
        for (Unit u : currentTurn.units()) {
            actualUnitsByProvince.put(u.location().provinceName(), u);
        }

        // Apply successful moves and remove dislodged units
        List<Unit> updatedUnits = new ArrayList<>(currentTurn.units());
        List<String> movesLog = new ArrayList<>();
        for (var entry : resolution.orderResults().entrySet()) {
            Order order = entry.getKey();
            if (order.type() == OrderType.MOVE
                    && entry.getValue() == OrderResult.SUCCESS) {
                Unit actualUnit = actualUnitsByProvince.get(order.source().provinceName());
                if (actualUnit != null) {
                    updatedUnits.remove(actualUnit);
                    order.target().ifPresent(target -> {
                        updatedUnits.add(actualUnit.relocatedTo(target));
                        provinceOwnership.put(target.provinceName(), actualUnit.nation());
                        movesLog.add(actualUnit.nation() + " " + actualUnit.unitType()
                                + " " + order.source().provinceName() + " -> " + target.provinceName());
                    });
                }
            }
        }
        if (!movesLog.isEmpty()) {
            log.info("Game {} | Successful moves: {}", gameId, String.join(", ", movesLog));
        }
        Set<String> dislodgedLog = new LinkedHashSet<>();
        for (Unit dislodged : resolution.dislodgementResult().dislodgedUnits().keySet()) {
            updatedUnits.remove(dislodged);
            dislodgedLog.add(dislodged.nation() + " " + dislodged.unitType()
                    + " in " + dislodged.location().provinceName());
        }
        if (!dislodgedLog.isEmpty()) {
            log.info("Game {} | Dislodged: {}", gameId, String.join(", ", dislodgedLog));
        }
        currentTurn = currentTurn.withUnits(updatedUnits);

        events.add(new PhaseEnded(gameId, currentTurn.phase().name(), resolution.dislodgementResult()));

        if (resolution.dislodgementResult().hasDislodgedUnits()) {
            turnHistory.add(currentTurn);
            currentTurn = currentTurn.withPhase(Phase.RETREAT);
            log.info("Game {} | Phase -> RETREAT ({})", gameId, currentTurn.season() + " " + currentTurn.year());
        } else if (currentTurn.season() == Season.AUTUMN) {
            turnHistory.add(currentTurn);
            currentTurn = currentTurn.withPhase(Phase.BUILD);
            log.info("Game {} | Phase -> BUILD ({})", gameId, currentTurn.season() + " " + currentTurn.year());
        } else {
            advanceToNextTurn();
            log.info("Game {} | Phase -> ORDERS ({})", gameId, currentTurn.season() + " " + currentTurn.year());
        }
        orderPool = new OrderPool();

        if (checkVictory()) {
            events.add(new GameFinished(gameId, winner, finalScores()));
            log.info("Game {} | VICTORY: {} wins with {} SCs",
                    gameId, winner, countControlledSupplyCenters().get(winner));
        }

        return resolution;
    }

    public void resolveRetreats(List<Order> retreatOrders, ConflictResolver resolver) {
        if (currentTurn.phase() != Phase.RETREAT) {
            throw new IllegalStateException("Not in RETREAT phase");
        }
        if (retreatOrders.stream().anyMatch(o -> o.type() != com.ulises.udiplomacy.domain.game.enums.OrderType.RETREAT
                && o.type() != com.ulises.udiplomacy.domain.game.enums.OrderType.DISBAND)) {
            throw new IllegalArgumentException("Only RETREAT and DISBAND orders allowed in retreat phase");
        }

        // Validate no duplicate units by province
        Set<String> seenProvinces = new HashSet<>();
        for (Order order : retreatOrders) {
            if (!seenProvinces.add(order.source().provinceName())) {
                throw new IllegalArgumentException("Duplicate retreat/disband for unit in "
                        + order.source().provinceName());
            }
        }

        Map<String, Unit> dislodgedUnitsByProvince = new HashMap<>();
        if (dislodgementResult != null) {
            for (Unit u : dislodgementResult.dislodgedUnits().keySet()) {
                dislodgedUnitsByProvince.put(u.location().provinceName(), u);
            }
        }

        var newUnits = new ArrayList<>(currentTurn.units());
        List<String> retreatLog = new ArrayList<>();
        List<String> disbandLog = new ArrayList<>();
        for (Order order : retreatOrders) {
            Unit dislodgedUnit = dislodgedUnitsByProvince.get(order.source().provinceName());
            if (order.type() == com.ulises.udiplomacy.domain.game.enums.OrderType.RETREAT) {
                order.target().ifPresent(target -> {
                    if (dislodgedUnit != null) {
                        var relocatedUnit = new Unit(order.unit().unitType(), dislodgedUnit.nation(),
                                new Territory(target.provinceName()));
                        newUnits.add(relocatedUnit);
                        provinceOwnership.put(target.provinceName(), dislodgedUnit.nation());
                        retreatLog.add(dislodgedUnit.nation() + " " + order.unit().unitType()
                                + " " + order.source().provinceName() + " -> " + target.provinceName());
                    }
                });
            } else {
                if (dislodgedUnit != null) {
                    disbandLog.add(dislodgedUnit.nation() + " " + order.unit().unitType()
                            + " in " + order.source().provinceName());
                }
            }
        }

        // Auto-generate DISBAND orders for dislodged units without a retreat/disband order
        List<Order> allResolvedOrders = new ArrayList<>(retreatOrders);
        Map<Order, OrderResult> retreatResults = new HashMap<>();
        for (Order order : retreatOrders) {
            retreatResults.put(order, OrderResult.SUCCESS);
        }
        for (var entry : dislodgedUnitsByProvince.entrySet()) {
            if (seenProvinces.contains(entry.getKey())) continue;
            Unit u = entry.getValue();
            Order disbandOrder = new Order(OrderType.DISBAND, u, u.location(), null, null);
            allResolvedOrders.add(disbandOrder);
            retreatResults.put(disbandOrder, OrderResult.SUCCESS);
            disbandLog.add(u.nation() + " " + u.unitType() + " in " + u.location().provinceName());
        }

        if (!retreatLog.isEmpty()) {
            log.info("Game {} | Retreats: {}", gameId, String.join(", ", retreatLog));
        }
        if (!disbandLog.isEmpty()) {
            log.info("Game {} | Disbands: {}", gameId, String.join(", ", disbandLog));
        }
        currentTurn = currentTurn.withUnits(newUnits);
        currentTurn = currentTurn.withOrdersResolved(allResolvedOrders, retreatResults);
        this.dislodgementResult = null;

        if (currentTurn.season() == Season.AUTUMN) {
            turnHistory.add(currentTurn);
            currentTurn = currentTurn.withPhase(Phase.BUILD);
            log.info("Game {} | Phase -> BUILD ({} {})", gameId, currentTurn.season(), currentTurn.year());
        } else {
            advanceToNextTurn();
        }

        if (checkVictory()) {
            events.add(new GameFinished(gameId, winner, finalScores()));
        }
    }

    public void resolveBuilds(List<Order> buildOrders) {
        if (currentTurn.phase() != Phase.BUILD) {
            throw new IllegalStateException("Not in BUILD phase");
        }
        if (buildOrders.stream().anyMatch(o -> o.type() != com.ulises.udiplomacy.domain.game.enums.OrderType.BUILD
                && o.type() != com.ulises.udiplomacy.domain.game.enums.OrderType.DISBAND)) {
            throw new IllegalArgumentException("Only BUILD and DISBAND orders allowed in build phase");
        }

        // Validate no duplicate provinces for builds, no duplicate units for disbands
        Set<String> buildProvinces = new HashSet<>();
        Set<String> disbandProvinces = new HashSet<>();
        for (Order order : buildOrders) {
            if (order.type() == com.ulises.udiplomacy.domain.game.enums.OrderType.BUILD) {
                String targetProv = order.target().map(Territory::provinceName).orElse(null);
                if (targetProv != null && !buildProvinces.add(targetProv)) {
                    throw new IllegalArgumentException("Duplicate build in province " + targetProv);
                }
            } else {
                if (!disbandProvinces.add(order.source().provinceName())) {
                    throw new IllegalArgumentException("Duplicate disband for unit in "
                            + order.source().provinceName());
                }
            }
        }

        // Pre-compute buildsAllowed per nation
        Map<Nation, Integer> buildsAllowedPerNation = new HashMap<>();
        Map<Nation, Integer> homeBuildsExecuted = new HashMap<>();
        for (Nation n : nations) {
            long controlledSCs = countControlledSupplyCenters().getOrDefault(n, 0L);
            long deployedUnits = currentTurn.units().stream()
                    .filter(u -> u.nation().equals(n))
                    .count();
            buildsAllowedPerNation.put(n, (int) Math.max(0, controlledSCs - deployedUnits));
            homeBuildsExecuted.put(n, 0);
        }

        Map<String, Unit> actualUnitsByProvince = new HashMap<>();
        for (Unit u : currentTurn.units()) {
            actualUnitsByProvince.put(u.location().provinceName(), u);
        }

        var newUnits = new ArrayList<>(currentTurn.units());
        List<String> buildLog = new ArrayList<>();
        List<String> disbandLog = new ArrayList<>();
        for (Order order : buildOrders) {
            if (order.type() == com.ulises.udiplomacy.domain.game.enums.OrderType.BUILD) {
                order.target().ifPresent(target -> {
                    var prov = gameMap.province(target.provinceName()).orElse(null);
                    if (prov == null || !prov.isSupplyCenter()) return;
                    if (order.unit().unitType() != UnitType.ARMY && !prov.isCoastal()) return;

                    Nation nation = null;
                    boolean isHomeBuild = false;
                    boolean isColonialBuild = false;

                    for (Nation n : nations) {
                        if (gameMap.homeCentersFor(n).stream()
                                .anyMatch(p -> p.name().equals(target.provinceName()))) {
                            nation = n;
                            isHomeBuild = true;
                            break;
                        }
                    }

                    if (nation == null && colonialRule) {
                        Nation owner = provinceOwnership.get(target.provinceName());
                        if (owner != null && unusedBuilds.getOrDefault(owner, 0) > 0) {
                            nation = owner;
                            isColonialBuild = true;
                        }
                    }

                    if (nation == null) return;

                    if (isHomeBuild) {
                        int built = homeBuildsExecuted.getOrDefault(nation, 0);
                        int allowed = buildsAllowedPerNation.getOrDefault(nation, 0);
                        if (built >= allowed) return;
                        homeBuildsExecuted.put(nation, built + 1);
                    }

                    if (isColonialBuild) {
                        int remaining = unusedBuilds.getOrDefault(nation, 0);
                        if (remaining <= 0) return;
                        unusedBuilds.put(nation, remaining - 1);
                    }

                    var newUnit = new Unit(order.unit().unitType(), nation,
                            new Territory(target.provinceName()));
                    newUnits.add(newUnit);
                    buildLog.add(nation + " " + order.unit().unitType()
                            + " in " + target.provinceName());
                });
            } else {
                Unit actualUnit = actualUnitsByProvince.get(order.source().provinceName());
                if (actualUnit != null) {
                    newUnits.remove(actualUnit);
                    disbandLog.add(actualUnit.nation() + " " + actualUnit.unitType()
                            + " in " + actualUnit.location().provinceName());
                }
            }
        }

        // Compute new unusedBuilds for next turn
        Map<Nation, Integer> nextUnusedBuilds = new HashMap<>();
        for (Nation n : nations) {
            int allowed = buildsAllowedPerNation.getOrDefault(n, 0);
            int executed = homeBuildsExecuted.getOrDefault(n, 0);
            nextUnusedBuilds.put(n, Math.max(0, allowed - executed));
        }
        this.unusedBuilds = nextUnusedBuilds;

        if (!buildLog.isEmpty()) {
            log.info("Game {} | Builds: {}", gameId, String.join(", ", buildLog));
        }
        if (!disbandLog.isEmpty()) {
            log.info("Game {} | Disbands: {}", gameId, String.join(", ", disbandLog));
        }
        currentTurn = currentTurn.withUnits(newUnits);
        Map<Order, OrderResult> buildResults = new HashMap<>();
        for (Order order : buildOrders) {
            buildResults.put(order, OrderResult.SUCCESS);
        }
        currentTurn = currentTurn.withOrdersResolved(buildOrders, buildResults);
        events.add(new TurnEnded(gameId, currentTurn.year(), currentTurn.season().name()));

        advanceToNextTurn();
        log.info("Game {} | Turn ended. New turn: {} {}", gameId, currentTurn.season(), currentTurn.year());

        if (checkVictory()) {
            events.add(new GameFinished(gameId, winner, finalScores()));
        }
    }

    public void advancePhase() {
        if (state != GameState.IN_PROGRESS) return;

        switch (currentTurn.phase()) {
            case RETREAT -> {
                // Record any unresolved dislodged units as DISBAND
                List<Order> resolvedOrders = new ArrayList<>();
                Map<Order, OrderResult> retreatResults = new HashMap<>();
                if (dislodgementResult != null) {
                    for (var entry : dislodgementResult.dislodgedUnits().entrySet()) {
                        Unit u = entry.getKey();
                        Order disbandOrder = new Order(OrderType.DISBAND, u, u.location(), null, null);
                        resolvedOrders.add(disbandOrder);
                        retreatResults.put(disbandOrder, OrderResult.SUCCESS);
                    }
                }
                if (!resolvedOrders.isEmpty()) {
                    currentTurn = currentTurn.withOrdersResolved(resolvedOrders, retreatResults);
                }
                this.dislodgementResult = null;
                turnHistory.add(currentTurn);
                if (currentTurn.season() == Season.AUTUMN) {
                    currentTurn = currentTurn.withPhase(Phase.BUILD);
                } else {
                    advanceToNextTurn();
                }
            }
            case BUILD -> {
                events.add(new TurnEnded(gameId, currentTurn.year(), currentTurn.season().name()));
                advanceToNextTurn();
            }
            case ORDERS -> {
            }
        }
    }

    public boolean checkVictory() {
        Map<Nation, Long> scControl = countControlledSupplyCenters();
        for (var entry : scControl.entrySet()) {
            if (entry.getValue() >= WIN_CONDITION_SCS) {
                winner = entry.getKey();
                state = GameState.FINISHED;
                return true;
            }
        }
        return false;
    }

    public Order removeOrder(int index) {
        if (state != GameState.IN_PROGRESS) {
            throw new IllegalStateException("Game is not in progress");
        }
        Order removed = orderPool.remove(index);
        log.debug("Game {} | Order removed [{}]: {} {} {}", gameId, index,
                removed.unit().unitType(), removed.type(), removed.source().provinceName());
        return removed;
    }

    public void undoLastTurn() {
        if (turnHistory.isEmpty()) {
            throw new IllegalStateException("No turns to undo");
        }
        currentTurn = turnHistory.removeLast();
        orderPool = new OrderPool();
        dislodgementResult = null;
        log.info("Game {} | Undo to {}", gameId, currentTurn.season() + " " + currentTurn.year() + " " + currentTurn.phase());
    }

    public void rewindToTurn(int turnIndex) {
        if (turnIndex < 0 || turnIndex >= turnHistory.size()) {
            throw new IllegalArgumentException("Invalid turn index: " + turnIndex);
        }
        currentTurn = turnHistory.get(turnIndex);
        turnHistory.subList(turnIndex, turnHistory.size()).clear();
        orderPool = new OrderPool();
        dislodgementResult = null;
        log.info("Game {} | Rewind to turn {}: {}", gameId, turnIndex, currentTurn.season() + " " + currentTurn.year());
    }

    public void autoFillHolds() {
        Map<String, Unit> unitsByProvince = new HashMap<>();
        for (Unit u : currentTurn.units()) {
            unitsByProvince.put(u.location().provinceName(), u);
        }

        // Replace stub units with actual game units so orders are logged with correct unit type
        List<Order> cleanedOrders = new ArrayList<>();
        for (Order order : orderPool.orders()) {
            Unit actualUnit = unitsByProvince.get(order.source().provinceName());
            if (actualUnit != null) {
                cleanedOrders.add(new Order(order.type(), actualUnit, order.source(),
                        order.target().orElse(null), order.auxiliary().orElse(null)));
            }
        }
        orderPool = new OrderPool(cleanedOrders);

        // Auto-fill holds for units without orders
        var orderedUnits = orderPool.orders().stream()
                .map(Order::unit)
                .toList();

        var unitsWithoutOrders = currentTurn.units().stream()
                .filter(u -> orderedUnits.stream().noneMatch(u::equals))
                .toList();

        if (!unitsWithoutOrders.isEmpty()) {
            log.debug("Game {} | Auto-fill holds for {} units: {}", gameId,
                    unitsWithoutOrders.size(),
                    unitsWithoutOrders.stream().map(u -> u.nation() + " " + u.unitType() + " " + u.location().provinceName()).toList());
        }

        for (Unit unit : unitsWithoutOrders) {
            orderPool.add(new Order(
                    com.ulises.udiplomacy.domain.game.enums.OrderType.HOLD,
                    unit, unit.location(), null, null));
        }
    }

    public BuildCapacity getBuildOptions(Nation nation) {
        long controlledSCs = countControlledSupplyCenters().getOrDefault(nation, 0L);
        long deployedUnits = currentTurn.units().stream()
                .filter(u -> u.nation().equals(nation))
                .count();
        int diff = (int) (controlledSCs - deployedUnits);
        int colonial = colonialRule ? unusedBuilds.getOrDefault(nation, 0) : 0;
        return new BuildCapacity(nation, Math.max(0, diff), Math.max(0, -diff), Math.max(0, colonial));
    }

    public Map<Nation, Long> countControlledSupplyCenters() {
        Map<Nation, Long> counts = new HashMap<>();
        for (Nation nation : nations) {
            counts.put(nation, 0L);
        }
        for (var entry : provinceOwnership.entrySet()) {
            if (gameMap.province(entry.getKey())
                    .map(Province::isSupplyCenter).orElse(false)) {
                counts.merge(entry.getValue(), 1L, Long::sum);
            }
        }
        return counts;
    }

    public Map<Nation, Integer> finalScores() {
        Map<Nation, Integer> scores = new HashMap<>();
        var counts = countControlledSupplyCenters();
        for (var entry : counts.entrySet()) {
            scores.put(entry.getKey(), entry.getValue().intValue());
        }
        return scores;
    }

    private void advanceToNextTurn() {
        turnHistory.add(currentTurn);
        Season nextSeason = currentTurn.season() == Season.SPRING ? Season.AUTUMN : Season.SPRING;
        int nextYear = currentTurn.season() == Season.AUTUMN ? currentTurn.year() + 1 : currentTurn.year();
        currentTurn = currentTurn.next(nextSeason, nextYear);
        this.dislodgementResult = null;
    }

    public String gameId() { return gameId; }
    public GameMap gameMap() { return gameMap; }
    public GameState state() { return state; }
    public Turn currentTurn() { return currentTurn; }
    public List<Turn> turnHistory() { return List.copyOf(turnHistory); }
    public Set<Nation> nations() { return Set.copyOf(nations); }
    public OrderPool orderPool() { return orderPool; }
    public Optional<DislodgementResult> dislodgementResult() {
        return Optional.ofNullable(dislodgementResult);
    }
    public Optional<Nation> winner() { return Optional.ofNullable(winner); }
    public List<DomainEvent> events() { return List.copyOf(events); }
    public void clearEvents() { events.clear(); }

    public void restore(GameState state, Turn currentTurn, List<Turn> turnHistory,
                         OrderPool orderPool, DislodgementResult dislodgementResult,
                         Nation winner, Map<String, Nation> provinceOwnership) {
        this.state = state;
        this.currentTurn = currentTurn;
        this.turnHistory.clear();
        this.turnHistory.addAll(turnHistory);
        this.orderPool = orderPool;
        this.dislodgementResult = dislodgementResult;
        this.winner = winner;
        this.provinceOwnership.clear();
        if (provinceOwnership != null) {
            this.provinceOwnership.putAll(provinceOwnership);
        }
        if (this.unusedBuilds == null) {
            this.unusedBuilds = new HashMap<>();
        }
    }

    public void restoreUnusedBuilds(Map<Nation, Integer> unusedBuilds) {
        this.unusedBuilds.clear();
        if (unusedBuilds != null) {
            this.unusedBuilds.putAll(unusedBuilds);
        }
    }

    public boolean colonialRule() { return colonialRule; }

    public Map<Nation, Integer> unusedBuilds() {
        return Collections.unmodifiableMap(unusedBuilds);
    }

    public Map<String, Nation> provinceOwnership() {
        return Collections.unmodifiableMap(provinceOwnership);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Game game)) return false;
        return Objects.equals(gameId, game.gameId);
    }

    @Override
    public int hashCode() { return Objects.hashCode(gameId); }
}
