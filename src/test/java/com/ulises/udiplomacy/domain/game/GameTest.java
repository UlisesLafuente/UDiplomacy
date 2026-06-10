package com.ulises.udiplomacy.domain.game;

import com.ulises.udiplomacy.domain.game.enums.GameState;
import com.ulises.udiplomacy.domain.game.enums.OrderResult;
import com.ulises.udiplomacy.domain.game.enums.OrderType;
import com.ulises.udiplomacy.domain.game.enums.Phase;
import com.ulises.udiplomacy.domain.game.enums.Season;
import com.ulises.udiplomacy.domain.game.enums.UnitType;
import com.ulises.udiplomacy.domain.game.events.GameFinished;
import com.ulises.udiplomacy.domain.game.events.GameStarted;
import com.ulises.udiplomacy.domain.game.events.PhaseEnded;
import com.ulises.udiplomacy.domain.game.events.TurnEnded;
import com.ulises.udiplomacy.domain.game.services.ConflictResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameTest {

    @Mock private ConflictResolver resolver;

    private GameMap map;
    private Game game;
    private Nation england;
    private Nation france;
    private Nation germany;
    private Unit englishArmy;
    private Unit frenchArmy;

    @BeforeEach
    void setUp() {
        map = TestMapLoader.loadClassic();
        england = new Nation("ENGLAND");
        france = new Nation("FRANCE");
        germany = new Nation("GERMANY");
        game = new Game("test-game", map, Set.of(
                england, france, germany
        ));
        englishArmy = new Unit(UnitType.ARMY, england, new Territory("LON"));
        frenchArmy = new Unit(UnitType.ARMY, france, new Territory("PAR"));
    }

    // --- Initial state ---

    @Test
    void initialGameState() {
        assertEquals(GameState.WAITING, game.state());
        assertEquals("test-game", game.gameId());
        assertEquals(3, game.nations().size());
        assertEquals(Season.SPRING, game.currentTurn().season());
        assertEquals(1901, game.currentTurn().year());
        assertEquals(Phase.ORDERS, game.currentTurn().phase());
        assertTrue(game.currentTurn().units().isEmpty());
        assertTrue(game.orderPool().orders().isEmpty());
        assertTrue(game.winner().isEmpty());
    }

    // --- start() ---

    @Test
    void startsGame() {
        game.start(List.of(englishArmy, frenchArmy));

        assertEquals(GameState.IN_PROGRESS, game.state());
        assertEquals(2, game.currentTurn().units().size());
        assertEquals(1, game.events().size());
        assertInstanceOf(GameStarted.class, game.events().get(0));
    }

    @Test
    void cannotStartGameTwice() {
        game.start(List.of(englishArmy));
        assertThrows(IllegalStateException.class, () -> game.start(List.of()));
    }

    // --- submitOrder() ---

    @Test
    void submitsOrder() {
        game.start(List.of(englishArmy));
        Order order = new Order(OrderType.HOLD, englishArmy, new Territory("LON"), null, null);
        game.submitOrder(order);
        assertEquals(1, game.orderPool().size());
    }

    @Test
    void cannotSubmitOrderBeforeStart() {
        Order order = new Order(OrderType.HOLD, englishArmy, new Territory("LON"), null, null);
        assertThrows(IllegalStateException.class, () -> game.submitOrder(order));
    }

    // --- executeOrders() ---

    @Test
    void executeOrders_autoFillsHolds() {
        game.start(List.of(englishArmy, frenchArmy));
        game.submitOrder(new Order(OrderType.HOLD, englishArmy, new Territory("LON"), null, null));

        when(resolver.resolve(any(), any(), any()))
                .thenReturn(new ResolutionResult(new DislodgementResult(Map.of(), Set.of()), Map.of()));

        game.executeOrders(resolver);

        Turn lastTurn = game.turnHistory().getLast();
        assertEquals(2, lastTurn.resolvedOrders().size());
    }

    @Test
    void executeOrders_advancesToRetreatPhase_whenDislodgements() {
        game.start(List.of(englishArmy, frenchArmy));
        game.submitOrder(new Order(OrderType.HOLD, englishArmy, new Territory("LON"), null, null));

        Unit dislodged = frenchArmy;
        ResolutionResult result = new ResolutionResult(
                new DislodgementResult(
                        Map.of(dislodged, List.of(new Territory("GAS"))), Set.of()), Map.of());
        when(resolver.resolve(any(), any(), any())).thenReturn(result);

        game.executeOrders(resolver);

        assertEquals(Phase.RETREAT, game.currentTurn().phase());
        assertTrue(game.dislodgementResult().isPresent());
        assertEquals(2, game.events().size());
        assertInstanceOf(PhaseEnded.class, game.events().get(1));
    }

    @Test
    void executeOrders_advancesToBuild_whenAutumnNoDislodgements() {
        game.start(List.of(englishArmy));
        // Advance past spring ORDERS to autumn
        when(resolver.resolve(any(), any(), any()))
                .thenReturn(new ResolutionResult(new DislodgementResult(Map.of(), Set.of()), Map.of()));
        game.executeOrders(resolver);

        assertEquals(Phase.ORDERS, game.currentTurn().phase());
        assertEquals(Season.AUTUMN, game.currentTurn().season());
        assertEquals(1901, game.currentTurn().year());

        // Now in autumn ORDERS
        game.submitOrder(new Order(OrderType.HOLD, englishArmy, new Territory("LON"), null, null));
        when(resolver.resolve(any(), any(), any()))
                .thenReturn(new ResolutionResult(new DislodgementResult(Map.of(), Set.of()), Map.of()));

        game.executeOrders(resolver);

        assertEquals(Phase.BUILD, game.currentTurn().phase());
    }

    @Test
    void executeOrders_advancesToNextTurn_whenSpringNoDislodgements() {
        game.start(List.of(englishArmy));
        when(resolver.resolve(any(), any(), any()))
                .thenReturn(new ResolutionResult(new DislodgementResult(Map.of(), Set.of()), Map.of()));

        game.executeOrders(resolver);

        assertEquals(Season.AUTUMN, game.currentTurn().season());
        assertEquals(1901, game.currentTurn().year());
        assertEquals(Phase.ORDERS, game.currentTurn().phase());
        assertEquals(1, game.turnHistory().size());
    }

    @Test
    void executeOrders_springTurnAfterAutumnBuild() {
        game.start(List.of(englishArmy));
        when(resolver.resolve(any(), any(), any()))
                .thenReturn(new ResolutionResult(new DislodgementResult(Map.of(), Set.of()), Map.of()));

        game.executeOrders(resolver);
        game.executeOrders(resolver);
        game.resolveBuilds(List.of());

        assertEquals(Season.SPRING, game.currentTurn().season());
        assertEquals(1902, game.currentTurn().year());
        assertEquals(Phase.ORDERS, game.currentTurn().phase());
    }

    @Test
    void executeOrders_cannotExecuteInRetreatPhase() {
        game.start(List.of(englishArmy));
        game.submitOrder(new Order(OrderType.HOLD, englishArmy, new Territory("LON"), null, null));
        when(resolver.resolve(any(), any(), any())).thenReturn(
                new ResolutionResult(new DislodgementResult(Map.of(englishArmy, List.of()), Set.of()), Map.of()));
        game.executeOrders(resolver);

        assertEquals(Phase.RETREAT, game.currentTurn().phase());
        assertThrows(IllegalStateException.class, () -> game.executeOrders(resolver));
    }

    // --- resolveRetreats() ---

    @Test
    void resolveRetreats_removesDislodgedUnit() {
        game.start(List.of(englishArmy, frenchArmy));
        game.submitOrder(new Order(OrderType.HOLD, englishArmy, new Territory("LON"), null, null));

        Unit dislodged = frenchArmy;
        when(resolver.resolve(any(), any(), any())).thenReturn(
                new ResolutionResult(new DislodgementResult(Map.of(dislodged, List.of(new Territory("GAS"))), Set.of()), Map.of()));
        game.executeOrders(resolver);

        assertEquals(Phase.RETREAT, game.currentTurn().phase());

        Order retreat = new Order(OrderType.RETREAT, dislodged,
                new Territory("PAR"), new Territory("GAS"), null);
        game.resolveRetreats(List.of(retreat), resolver);

        assertTrue(game.currentTurn().units().stream()
                .anyMatch(u -> u.location().provinceName().equals("GAS")));
        assertTrue(game.currentTurn().units().stream()
                .noneMatch(u -> u.location().provinceName().equals("PAR")));
    }

    @Test
    void resolveRetreats_disbandRemovesUnit() {
        game.start(List.of(englishArmy, frenchArmy));
        game.submitOrder(new Order(OrderType.HOLD, englishArmy, new Territory("LON"), null, null));

        when(resolver.resolve(any(), any(), any())).thenReturn(
                new ResolutionResult(new DislodgementResult(Map.of(frenchArmy, List.of(new Territory("GAS"))), Set.of()), Map.of()));
        game.executeOrders(resolver);

        Order disband = new Order(OrderType.DISBAND, frenchArmy, new Territory("PAR"), null, null);
        game.resolveRetreats(List.of(disband), resolver);

        assertTrue(game.currentTurn().units().stream()
                .noneMatch(u -> u.location().provinceName().equals("PAR")));
    }

    @Test
    void resolveRetreats_rejectsNonRetreatOrders() {
        game.start(List.of(englishArmy));
        when(resolver.resolve(any(), any(), any())).thenReturn(
                new ResolutionResult(new DislodgementResult(Map.of(englishArmy, List.of()), Set.of()), Map.of()));
        game.executeOrders(resolver);

        assertThrows(IllegalArgumentException.class, () ->
                game.resolveRetreats(List.of(
                        new Order(OrderType.HOLD, englishArmy, new Territory("LON"), null, null)
                ), resolver));
    }

    // --- resolveBuilds() ---

    @Test
    void resolveBuilds_createsNewUnit() {
        game.start(List.of(englishArmy));
        // Advance to BUILD phase
        when(resolver.resolve(any(), any(), any()))
                .thenReturn(new ResolutionResult(new DislodgementResult(Map.of(), Set.of()), Map.of()));
        game.executeOrders(resolver);
        game.executeOrders(resolver);

        assertEquals(Phase.BUILD, game.currentTurn().phase());

        Unit newUnit = new Unit(UnitType.FLEET, england, new Territory("LON"));
        Order build = new Order(OrderType.BUILD, newUnit,
                new Territory("LON"), new Territory("LON"), null);
        game.resolveBuilds(List.of(build));

        assertTrue(game.currentTurn().units().stream()
                .anyMatch(u -> u.location().provinceName().equals("LON")
                        && u.nation().equals(england)));
    }

    @Test
    void resolveBuilds_disbandRemovesUnit() {
        game.start(List.of(englishArmy, frenchArmy));
        when(resolver.resolve(any(), any(), any()))
                .thenReturn(new ResolutionResult(new DislodgementResult(Map.of(), Set.of()), Map.of()));
        game.executeOrders(resolver);
        game.executeOrders(resolver);

        Order disband = new Order(OrderType.DISBAND, frenchArmy, new Territory("PAR"), null, null);
        game.resolveBuilds(List.of(disband));

        assertTrue(game.currentTurn().units().stream()
                .noneMatch(u -> u.equals(frenchArmy)));
    }

    // --- victory ---

    @Test
    void checkVictory_18SCsWins() {
        // Create units occupying 18+ supply centers
        List<Unit> units = map.supplyCenterNames().stream()
                .limit(18)
                .map(sc -> new Unit(UnitType.ARMY, england, new Territory(sc)))
                .toList();
        game = new Game("victory-game", map, Set.of(england, france));
        game.start(units);
        when(resolver.resolve(any(), any(), any()))
                .thenReturn(new ResolutionResult(new DislodgementResult(Map.of(), Set.of()), Map.of()));

        game.executeOrders(resolver);

        assertTrue(game.winner().isPresent());
        assertEquals(england, game.winner().get());
        assertEquals(GameState.FINISHED, game.state());
        assertInstanceOf(GameFinished.class, game.events().getLast());
    }

    @Test
    void checkVictory_lessThan18_noWinner() {
        // Create units in only 5 supply centers
        List<Unit> units = map.supplyCenterNames().stream()
                .limit(5)
                .map(sc -> new Unit(UnitType.ARMY, england, new Territory(sc)))
                .toList();
        game.start(units);
        when(resolver.resolve(any(), any(), any()))
                .thenReturn(new ResolutionResult(new DislodgementResult(Map.of(), Set.of()), Map.of()));

        game.executeOrders(resolver);

        assertTrue(game.winner().isEmpty());
    }

    // --- undo ---

    @Test
    void undoLastTurn_restoresPreviousTurn() {
        game.start(List.of(englishArmy));
        when(resolver.resolve(any(), any(), any()))
                .thenReturn(new ResolutionResult(new DislodgementResult(Map.of(), Set.of()), Map.of()));
        game.executeOrders(resolver);

        assertEquals(Season.AUTUMN, game.currentTurn().season());

        game.undoLastTurn();

        assertEquals(Season.SPRING, game.currentTurn().season());
        assertEquals(1901, game.currentTurn().year());
    }

    @Test
    void undoLastTurn_failsWhenNoHistory() {
        assertThrows(IllegalStateException.class, () -> game.undoLastTurn());
    }

    // --- build options ---

    @Test
    void getBuildOptions_returnsDisbandsWhenMoreUnitsThanSCs() {
        // England controls LON, EDI, LVP (3 home SCs) with 4 units -> 1 disband, 0 builds
        game.start(List.of(
                new Unit(UnitType.ARMY, england, new Territory("LON")),
                new Unit(UnitType.FLEET, england, new Territory("EDI")),
                new Unit(UnitType.ARMY, england, new Territory("CLY")),
                new Unit(UnitType.ARMY, england, new Territory("YOR"))
        ));
        BuildCapacity capacity = game.getBuildOptions(england);
        assertEquals(england, capacity.nation());
        assertEquals(0, capacity.buildsAllowed());
        assertEquals(1, capacity.disbandsRequired());
    }

    @Test
    void getBuildOptions_disbandsWhenMoreUnitsThanSCs() {
        // England controls LON, EDI, LVP (3 home SCs) with 4 units -> 1 disband, 0 builds
        game.start(List.of(
                new Unit(UnitType.ARMY, england, new Territory("LON")),
                new Unit(UnitType.ARMY, england, new Territory("EDI")),
                new Unit(UnitType.ARMY, england, new Territory("YOR")),
                new Unit(UnitType.ARMY, england, new Territory("CLY"))
        ));
        BuildCapacity capacity = game.getBuildOptions(england);
        assertEquals(1, capacity.disbandsRequired());
        assertEquals(0, capacity.buildsAllowed());
    }

    // --- events ---

    @Test
    void startGame_emitsGameStarted() {
        game.start(List.of(englishArmy));
        assertEquals(1, game.events().stream()
                .filter(e -> e instanceof GameStarted).count());
    }

    @Test
    void executeOrders_emitsPhaseEnded() {
        game.start(List.of(englishArmy));
        when(resolver.resolve(any(), any(), any()))
                .thenReturn(new ResolutionResult(new DislodgementResult(Map.of(), Set.of()), Map.of()));
        game.executeOrders(resolver);
        assertEquals(1, game.events().stream()
                .filter(e -> e instanceof PhaseEnded).count());
    }

    @Test
    void resolveBuilds_emitsTurnEnded() {
        game.start(List.of(englishArmy));
        when(resolver.resolve(any(), any(), any()))
                .thenReturn(new ResolutionResult(new DislodgementResult(Map.of(), Set.of()), Map.of()));
        game.executeOrders(resolver);
        game.executeOrders(resolver);
        game.resolveBuilds(List.of());
        assertEquals(1, game.events().stream()
                .filter(e -> e instanceof TurnEnded).count());
    }

    // --- countControlledSupplyCenters ---

    @Test
    void countControlledSupplyCenters_allHomeSCsOwnedAtStart() {
        game.start(List.of());
        assertEquals(22, game.countControlledSupplyCenters().values().stream()
                .mapToLong(Long::longValue).sum());
    }

    @Test
    void countControlledSupplyCenters_countsOwnedSCs() {
        game.start(List.of(
                new Unit(UnitType.ARMY, england, new Territory("LON")),
                new Unit(UnitType.ARMY, england, new Territory("PAR"))
        ));
        var counts = game.countControlledSupplyCenters();
        assertEquals(4, counts.get(england).longValue());
        assertEquals(2, counts.get(france).longValue());
    }

    // --- advancePhase ---

    @Test
    void advancePhase_inRetreat_goesToOrders() {
        game.start(List.of(englishArmy));
        game.submitOrder(new Order(OrderType.HOLD, englishArmy, new Territory("LON"), null, null));
        when(resolver.resolve(any(), any(), any())).thenReturn(
                new ResolutionResult(new DislodgementResult(Map.of(englishArmy, List.of()), Set.of()), Map.of()));
        game.executeOrders(resolver);

        assertEquals(Phase.RETREAT, game.currentTurn().phase());

        game.advancePhase();

        assertEquals(Phase.ORDERS, game.currentTurn().phase());
    }

    @Test
    void advancePhase_inBuild_advancesTurn() {
        game.start(List.of(englishArmy));
        when(resolver.resolve(any(), any(), any()))
                .thenReturn(new ResolutionResult(new DislodgementResult(Map.of(), Set.of()), Map.of()));
        game.executeOrders(resolver);

        assertEquals(Season.AUTUMN, game.currentTurn().season());

        game.executeOrders(resolver);
        assertEquals(Phase.BUILD, game.currentTurn().phase());

        game.advancePhase();

        assertEquals(Season.SPRING, game.currentTurn().season());
        assertEquals(1902, game.currentTurn().year());
    }

    // --- unit type validation ---

    @Test
    void autoFillHolds_correctsWrongUnitTypeAndLogsFailure() {
        var fleet = new Unit(UnitType.FLEET, england, new Territory("ROM"));
        game.start(List.of(fleet));
        // Submit order with wrong unit type (ARMY, but actual unit is FLEET)
        var wrongOrder = new Order(OrderType.MOVE,
                new Unit(UnitType.ARMY, england, new Territory("ROM")),
                new Territory("ROM"), new Territory("APU"), null);
        game.submitOrder(wrongOrder);

        var realResolver = new ConflictResolver();
        game.executeOrders(realResolver);

        // The order should be in the turn history (logged) with FAILURE
        Turn histTurn = game.turnHistory().getLast();
        var failedOrder = histTurn.resolvedOrders().stream()
                .filter(o -> o.source().provinceName().equals("ROM"))
                .findFirst();
        assertTrue(failedOrder.isPresent(), "Order should be logged in history");
        assertEquals(UnitType.FLEET, failedOrder.get().unit().unitType(),
                "Unit type should be corrected to FLEET in history");
        assertEquals(OrderResult.FAILURE, histTurn.resolvedResults().get(failedOrder.get()),
                "FLEET ROM -> APU should fail (not a valid fleet route)");

        // FLEET should stay in ROM
        assertTrue(game.currentTurn().units().stream()
                .anyMatch(u -> u.location().provinceName().equals("ROM")
                        && u.unitType() == UnitType.FLEET),
                "FLEET in ROM should still be there");
        assertTrue(game.currentTurn().units().stream()
                .noneMatch(u -> u.location().provinceName().equals("APU")),
                "No unit should have moved to APU");
    }

    @Test
    void autoFillHolds_acceptsOrderWithCorrectUnitType() {
        var fleet = new Unit(UnitType.FLEET, england, new Territory("ROM"));
        game.start(List.of(fleet));
        // Submit order with correct unit type (FLEET) to NAP (both share TYS sea)
        var correctOrder = new Order(OrderType.MOVE, fleet,
                new Territory("ROM"), new Territory("NAP"), null);
        game.submitOrder(correctOrder);

        var realResolver = new ConflictResolver();
        game.executeOrders(realResolver);

        // The FLEET in ROM should have moved to NAP
        assertTrue(game.currentTurn().units().stream()
                .anyMatch(u -> u.location().provinceName().equals("NAP")
                        && u.unitType() == UnitType.FLEET),
                "FLEET should have moved to NAP");
    }

    @Test
    void provinceOwnership_retainsSourceAfterSuccessfulMove() {
        game.start(List.of(englishArmy));
        // Move LON -> WAL (Wales is coastal, adjacent to LON)
        var move = new Order(OrderType.MOVE, englishArmy, new Territory("LON"), new Territory("WAL"), null);
        game.submitOrder(move);

        when(resolver.resolve(any(), any(), any())).thenReturn(
                new ResolutionResult(new DislodgementResult(Map.of(), Set.of()),
                        Map.of(move, OrderResult.SUCCESS)));
        game.executeOrders(resolver);

        assertTrue(game.provinceOwnership().containsKey("LON"),
                "Source province should remain in provinceOwnership after unit leaves");
        assertEquals(england, game.provinceOwnership().get("LON"),
                "Source province should still be owned by the original nation");
        assertTrue(game.provinceOwnership().containsKey("WAL"),
                "Target province should be added to provinceOwnership");
        assertEquals(england, game.provinceOwnership().get("WAL"),
                "Target province should be owned by the moving unit's nation");
    }
}
