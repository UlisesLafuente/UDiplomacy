package com.ulises.udiplomacy.domain.game;

import com.ulises.udiplomacy.domain.game.enums.GameState;
import com.ulises.udiplomacy.domain.game.enums.Phase;
import com.ulises.udiplomacy.domain.game.enums.Season;
import com.ulises.udiplomacy.domain.game.enums.UnitType;
import com.ulises.udiplomacy.domain.game.enums.OrderResult;
import com.ulises.udiplomacy.domain.game.events.DomainEvent;
import com.ulises.udiplomacy.domain.game.events.GameFinished;
import com.ulises.udiplomacy.domain.game.events.GameStarted;
import com.ulises.udiplomacy.domain.game.events.PhaseEnded;
import com.ulises.udiplomacy.domain.game.events.TurnEnded;
import com.ulises.udiplomacy.domain.game.services.ConflictResolver;

import java.util.*;

public final class Game {
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

    public Game(String gameId, GameMap gameMap, Set<Nation> nations) {
        this.gameId = gameId;
        this.gameMap = gameMap;
        this.nations = new HashSet<>(nations);
        this.state = GameState.WAITING;
        this.orderPool = new OrderPool();
        this.turnHistory = new ArrayList<>();
        this.events = new ArrayList<>();
        this.currentTurn = new Turn(Season.SPRING, 1901, Phase.ORDERS, List.of());
    }

    public void start(List<Unit> initialUnits) {
        if (state != GameState.WAITING) {
            throw new IllegalStateException("Game already started");
        }
        state = GameState.IN_PROGRESS;
        currentTurn = new Turn(Season.SPRING, 1901, Phase.ORDERS, initialUnits);
        events.add(new GameStarted(gameId, new HashSet<>(nations)));
    }

    public void submitOrder(Order order) {
        if (state != GameState.IN_PROGRESS) {
            throw new IllegalStateException("Game is not in progress");
        }
        orderPool.add(order);
    }

    public ResolutionResult executeOrders(ConflictResolver resolver) {
        if (state != GameState.IN_PROGRESS) {
            throw new IllegalStateException("Game is not in progress");
        }
        if (currentTurn.phase() != Phase.ORDERS) {
            throw new IllegalStateException("Can only execute orders in ORDERS phase");
        }

        autoFillHolds();
        ResolutionResult resolution = resolver.resolve(orderPool.orders(), currentTurn.units(), gameMap);
        this.dislodgementResult = resolution.dislodgementResult();

        currentTurn = currentTurn.withOrdersResolved(orderPool.orders(), resolution.orderResults());
        events.add(new PhaseEnded(gameId, currentTurn.phase().name(), resolution.dislodgementResult()));

        if (resolution.dislodgementResult().hasDislodgedUnits()) {
            currentTurn = currentTurn.withPhase(Phase.RETREAT);
        } else if (currentTurn.season() == Season.AUTUMN) {
            currentTurn = currentTurn.withPhase(Phase.BUILD);
        } else {
            advanceToNextTurn();
        }
        orderPool = new OrderPool();

        if (checkVictory()) {
            events.add(new GameFinished(gameId, winner, finalScores()));
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

        var newUnits = new ArrayList<>(currentTurn.units());
        for (Order order : retreatOrders) {
            newUnits.remove(order.unit());
            if (order.type() == com.ulises.udiplomacy.domain.game.enums.OrderType.RETREAT) {
                order.target().ifPresent(target ->
                        newUnits.add(order.unit().relocatedTo(target)));
            }
        }
        currentTurn = currentTurn.withUnits(newUnits);
        this.dislodgementResult = null;

        if (currentTurn.season() == Season.AUTUMN) {
            currentTurn = currentTurn.withPhase(Phase.BUILD);
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

        var newUnits = new ArrayList<>(currentTurn.units());
        for (Order order : buildOrders) {
            if (order.type() == com.ulises.udiplomacy.domain.game.enums.OrderType.BUILD) {
                order.target().ifPresent(target -> {
                    if (gameMap.province(target.provinceName())
                            .map(p -> p.isCoastal() == (order.unit().unitType() == UnitType.FLEET)
                                    || p.isInland())
                            .orElse(false)) {
                        newUnits.add(order.unit());
                    }
                });
            } else {
                newUnits.remove(order.unit());
            }
        }
        currentTurn = currentTurn.withUnits(newUnits);
        events.add(new TurnEnded(gameId, currentTurn.year(), currentTurn.season().name()));

        advanceToNextTurn();

        if (checkVictory()) {
            events.add(new GameFinished(gameId, winner, finalScores()));
        }
    }

    public void advancePhase() {
        if (state != GameState.IN_PROGRESS) return;

        switch (currentTurn.phase()) {
            case RETREAT -> {
                currentTurn = currentTurn.withPhase(Phase.ORDERS);
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
        return orderPool.remove(index);
    }

    public void undoLastTurn() {
        if (turnHistory.isEmpty()) {
            throw new IllegalStateException("No turns to undo");
        }
        currentTurn = turnHistory.removeLast();
        orderPool = new OrderPool();
        dislodgementResult = null;
    }

    public void rewindToTurn(int turnIndex) {
        if (turnIndex < 0 || turnIndex >= turnHistory.size()) {
            throw new IllegalArgumentException("Invalid turn index: " + turnIndex);
        }
        currentTurn = turnHistory.get(turnIndex);
        turnHistory.subList(turnIndex, turnHistory.size()).clear();
        orderPool = new OrderPool();
        dislodgementResult = null;
    }

    public void autoFillHolds() {
        var existingOrders = orderPool.orders();
        var orderedUnits = existingOrders.stream()
                .map(Order::unit)
                .toList();

        var unitsWithoutOrders = currentTurn.units().stream()
                .filter(u -> orderedUnits.stream().noneMatch(u::equals))
                .toList();

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
        return new BuildCapacity(nation, Math.max(0, diff), Math.max(0, -diff));
    }

    public Map<Nation, Long> countControlledSupplyCenters() {
        Map<Nation, Long> counts = new HashMap<>();
        for (Nation nation : nations) {
            counts.put(nation, 0L);
        }
        for (Unit unit : currentTurn.units()) {
            if (gameMap.province(unit.location().provinceName())
                    .map(Province::isSupplyCenter).orElse(false)) {
                counts.merge(unit.nation(), 1L, Long::sum);
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
                         Nation winner) {
        this.state = state;
        this.currentTurn = currentTurn;
        this.turnHistory.clear();
        this.turnHistory.addAll(turnHistory);
        this.orderPool = orderPool;
        this.dislodgementResult = dislodgementResult;
        this.winner = winner;
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
