package com.ulises.udiplomacy.domain.game;

import com.ulises.udiplomacy.domain.game.enums.OrderType;
import com.ulises.udiplomacy.domain.game.enums.Phase;
import com.ulises.udiplomacy.domain.game.enums.Season;
import com.ulises.udiplomacy.domain.game.enums.UnitType;
import com.ulises.udiplomacy.domain.game.services.ConflictResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

class MatchSimulationTest {

    private GameMap map;
    private ConflictResolver resolver;
    private Nation england, france, germany, italy, austria, russia, turkey;

    @BeforeEach
    void setUp() {
        map = TestMapLoader.loadClassic();
        resolver = new ConflictResolver();
        england = new Nation("ENGLAND");
        france = new Nation("FRANCE");
        germany = new Nation("GERMANY");
        italy = new Nation("ITALY");
        austria = new Nation("AUSTRIA");
        russia = new Nation("RUSSIA");
        turkey = new Nation("TURKEY");
    }

    private Game createGame() {
        return new Game("match-test", map, Set.of(england, france, germany, italy, austria, russia, turkey));
    }

    private List<Unit> startingUnits() {
        return List.of(
                new Unit(UnitType.FLEET, england, new Territory("EDI")),
                new Unit(UnitType.ARMY, england, new Territory("LVP")),
                new Unit(UnitType.FLEET, england, new Territory("LON")),
                new Unit(UnitType.ARMY, france, new Territory("PAR")),
                new Unit(UnitType.ARMY, france, new Territory("MAR")),
                new Unit(UnitType.FLEET, france, new Territory("BRE")),
                new Unit(UnitType.ARMY, germany, new Territory("BER")),
                new Unit(UnitType.ARMY, germany, new Territory("MUN")),
                new Unit(UnitType.FLEET, germany, new Territory("KIE")),
                new Unit(UnitType.ARMY, italy, new Territory("ROM")),
                new Unit(UnitType.ARMY, italy, new Territory("VEN")),
                new Unit(UnitType.FLEET, italy, new Territory("NAP")),
                new Unit(UnitType.ARMY, austria, new Territory("VIE")),
                new Unit(UnitType.ARMY, austria, new Territory("BUD")),
                new Unit(UnitType.FLEET, austria, new Territory("TRI")),
                new Unit(UnitType.ARMY, russia, new Territory("MOS")),
                new Unit(UnitType.ARMY, russia, new Territory("WAR")),
                new Unit(UnitType.FLEET, russia, new Territory("STP", new Coast("south"))),
                new Unit(UnitType.FLEET, russia, new Territory("SEV")),
                new Unit(UnitType.ARMY, turkey, new Territory("CON")),
                new Unit(UnitType.ARMY, turkey, new Territory("ANK")),
                new Unit(UnitType.FLEET, turkey, new Territory("SMY"))
        );
    }

    private void submitOrders(Game game, List<Order> orders) {
        Map<String, Unit> unitsByProvince = new HashMap<>();
        for (Unit u : game.currentTurn().units()) {
            unitsByProvince.put(u.location().provinceName(), u);
        }
        for (Order o : orders) {
            String src = o.source().provinceName();
            Unit unit = unitsByProvince.get(src);
            if (unit != null) {
                game.submitOrder(new Order(o.type(), unit, o.source(),
                        o.target().orElse(null), o.auxiliary().orElse(null)));
            }
        }
    }

    private void processNextPhase(Game game) {
        var phase = game.currentTurn().phase();
        System.out.println("  Phase: " + phase);

        if (phase == Phase.RETREAT) {
            List<Order> disbands = new ArrayList<>();
            var dislodged = game.dislodgementResult()
                    .orElseThrow(() -> new IllegalStateException("No dislodgement result in RETREAT"));
            for (Unit u : dislodged.dislodgedUnits().keySet()) {
                disbands.add(new Order(OrderType.DISBAND, u, u.location(), null, null));
                System.out.println("  Disband " + u.unitType() + " in " + u.location().provinceName());
            }
            game.resolveRetreats(disbands, resolver);
            processNextPhase(game);
        } else if (phase == Phase.BUILD) {
            game.advancePhase();
            processNextPhase(game);
        }
    }

    private List<Order> spring1901Orders() {
        return List.of(
                new Order(OrderType.MOVE, null, new Territory("EDI"), new Territory("NWG"), null),
                new Order(OrderType.MOVE, null, new Territory("LON"), new Territory("NTH"), null),
                new Order(OrderType.MOVE, null, new Territory("LVP"), new Territory("YOR"), null),
                new Order(OrderType.MOVE, null, new Territory("BRE"), new Territory("MID"), null),
                new Order(OrderType.MOVE, null, new Territory("PAR"), new Territory("BUR"), null),
                new Order(OrderType.MOVE, null, new Territory("MAR"), new Territory("SPA"), null),
                new Order(OrderType.MOVE, null, new Territory("KIE"), new Territory("DEN"), null),
                new Order(OrderType.MOVE, null, new Territory("BER"), new Territory("KIE"), null),
                new Order(OrderType.MOVE, null, new Territory("MUN"), new Territory("RUH"), null),
                new Order(OrderType.MOVE, null, new Territory("NAP"), new Territory("ION"), null),
                new Order(OrderType.MOVE, null, new Territory("ROM"), new Territory("VEN"), null),
                new Order(OrderType.HOLD, null, new Territory("VEN"), null, null),
                new Order(OrderType.MOVE, null, new Territory("TRI"), new Territory("ALB"), null),
                new Order(OrderType.MOVE, null, new Territory("VIE"), new Territory("GAL"), null),
                new Order(OrderType.MOVE, null, new Territory("BUD"), new Territory("SER"), null),
                new Order(OrderType.MOVE, null, new Territory("STP"), new Territory("BOT"), null),
                new Order(OrderType.MOVE, null, new Territory("MOS"), new Territory("UKR"), null),
                new Order(OrderType.MOVE, null, new Territory("WAR"), new Territory("GAL"), null),
                new Order(OrderType.MOVE, null, new Territory("SEV"), new Territory("BLA"), null),
                new Order(OrderType.MOVE, null, new Territory("SMY"), new Territory("EAS"), null),
                new Order(OrderType.MOVE, null, new Territory("CON"), new Territory("BUL"), null),
                new Order(OrderType.MOVE, null, new Territory("ANK"), new Territory("CON"), null));
    }

    private List<Order> fall1901Orders() {
        return List.of(
                new Order(OrderType.MOVE, null, new Territory("NWG"), new Territory("NOR"), null),
                new Order(OrderType.CONVOY, null, new Territory("NTH"), new Territory("NOR"), new Territory("YOR")),
                new Order(OrderType.MOVE, null, new Territory("YOR"), new Territory("NOR"), null),
                new Order(OrderType.MOVE, null, new Territory("MID"), new Territory("POR"), null),
                new Order(OrderType.MOVE, null, new Territory("BUR"), new Territory("BEL"), null),
                new Order(OrderType.HOLD, null, new Territory("SPA"), null, null),
                new Order(OrderType.MOVE, null, new Territory("DEN"), new Territory("SWE"), null),
                new Order(OrderType.MOVE, null, new Territory("KIE"), new Territory("HOL"), null),
                new Order(OrderType.MOVE, null, new Territory("RUH"), new Territory("BEL"), null),
                new Order(OrderType.MOVE, null, new Territory("ION"), new Territory("TUN"), null),
                new Order(OrderType.MOVE, null, new Territory("VEN"), new Territory("TRI"), null),
                new Order(OrderType.MOVE, null, new Territory("ALB"), new Territory("GRE"), null),
                new Order(OrderType.HOLD, null, new Territory("GAL"), null, null),
                new Order(OrderType.HOLD, null, new Territory("SER"), null, null),
                new Order(OrderType.MOVE, null, new Territory("BOT"), new Territory("SWE"), null),
                new Order(OrderType.MOVE, null, new Territory("UKR"), new Territory("RUM"), null),
                new Order(OrderType.MOVE, null, new Territory("GAL"), new Territory("VIE"), null),
                new Order(OrderType.MOVE, null, new Territory("BLA"), new Territory("RUM"), null),
                new Order(OrderType.SUPPORT, null, new Territory("BLA"), new Territory("RUM"), new Territory("BUL")),
                new Order(OrderType.MOVE, null, new Territory("BUL"), new Territory("RUM"), null),
                new Order(OrderType.HOLD, null, new Territory("CON"), null, null));
    }

    @Test
    void processSpringAndFall1901() {
        Game game = createGame();
        game.start(startingUnits());
        assertEquals(Season.SPRING, game.currentTurn().season());
        assertEquals(1901, game.currentTurn().year());
        assertEquals(22, game.currentTurn().units().size());

        // SPRING 1901
        submitOrders(game, spring1901Orders());
        var sprResult = game.executeOrders(resolver);
        assertNotNull(sprResult);
        processNextPhase(game);
        assertEquals(Phase.ORDERS, game.currentTurn().phase());
        assertEquals(Season.AUTUMN, game.currentTurn().season());
        assertEquals(1901, game.currentTurn().year());

        // FALL 1901
        int unitCountBeforeFall = game.currentTurn().units().size();
        assertTrue(unitCountBeforeFall >= 18, "Should still have most units after Spring");

        submitOrders(game, fall1901Orders());
        var fallResult = game.executeOrders(resolver);
        assertNotNull(fallResult);
        processNextPhase(game);

        // Should now be in SPRING 1902 ORDERS
        assertEquals(Phase.ORDERS, game.currentTurn().phase());
        assertEquals(Season.SPRING, game.currentTurn().season());
        assertEquals(1902, game.currentTurn().year());
        assertTrue(game.currentTurn().units().size() >= 16, "Game should still have most units");
        System.out.println("Final units after FALL 1901: " + game.currentTurn().units().size());
    }
}
