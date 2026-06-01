package com.ulises.udiplomacy.domain.game.services;

import com.ulises.udiplomacy.domain.game.*;
import com.ulises.udiplomacy.domain.game.enums.OrderResult;
import com.ulises.udiplomacy.domain.game.enums.OrderType;
import com.ulises.udiplomacy.domain.game.enums.UnitType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;

class ConflictResolverTest {

    private ConflictResolver resolver;
    private GameMap map;
    private Nation germany;
    private Nation france;
    private Nation austria;
    private Nation russia;
    private Nation turkey;

    @BeforeEach
    void setUp() {
        resolver = new ConflictResolver();
        map = TestMapLoader.loadClassic();
        germany = new Nation("GERMANY");
        france = new Nation("FRANCE");
        austria = new Nation("AUSTRIA");
        russia = new Nation("RUSSIA");
        turkey = new Nation("TURKEY");
    }

    @Test
    void allHold_noDislodgements() {
        var units = List.of(
                new Unit(UnitType.ARMY, germany, new Territory("MUN")),
                new Unit(UnitType.FLEET, france, new Territory("BRE"))
        );
        var orders = List.of(
                new Order(OrderType.HOLD, units.get(0), new Territory("MUN"), null, null),
                new Order(OrderType.HOLD, units.get(1), new Territory("BRE"), null, null)
        );
        ResolutionResult result = resolver.resolve(orders, units, map);
        assertTrue(result.dislodgementResult().dislodgedUnits().isEmpty());
        assertTrue(result.dislodgementResult().contestedProvinces().isEmpty());
        assertEquals(2, result.orderResults().size());
        result.orderResults().values().forEach(r -> assertEquals(OrderResult.SUCCESS, r));
    }

    @Test
    void moveToEmptyProvince_noDislodgements() {
        var unit = new Unit(UnitType.ARMY, germany, new Territory("MUN"));
        var orders = List.of(
                new Order(OrderType.MOVE, unit, new Territory("MUN"), new Territory("BUR"), null)
        );
        ResolutionResult result = resolver.resolve(orders, List.of(unit), map);
        assertTrue(result.dislodgementResult().dislodgedUnits().isEmpty());
        assertTrue(result.dislodgementResult().contestedProvinces().isEmpty());
    }

    @Test
    void unsupportedAttackOnHold_fails() {
        var attacker = new Unit(UnitType.ARMY, germany, new Territory("MUN"));
        var defender = new Unit(UnitType.ARMY, france, new Territory("BUR"));
        var orders = List.of(
                new Order(OrderType.MOVE, attacker, new Territory("MUN"), new Territory("BUR"), null),
                new Order(OrderType.HOLD, defender, new Territory("BUR"), null, null)
        );
        ResolutionResult result = resolver.resolve(orders, List.of(attacker, defender), map);
        assertTrue(result.dislodgementResult().dislodgedUnits().isEmpty());
        assertTrue(result.dislodgementResult().contestedProvinces().isEmpty());
    }

    @Test
    void supportedAttack_dislodgesDefender() {
        var attacker = new Unit(UnitType.ARMY, germany, new Territory("MUN"));
        var supporter = new Unit(UnitType.ARMY, germany, new Territory("RUH"));
        var defender = new Unit(UnitType.ARMY, france, new Territory("BUR"));

        Order moveOrder = new Order(OrderType.MOVE, attacker,
                new Territory("MUN"), new Territory("BUR"), null);
        Order supportOrder = new Order(OrderType.SUPPORT, supporter,
                new Territory("RUH"), new Territory("BUR"), new Territory("MUN"));
        Order holdOrder = new Order(OrderType.HOLD, defender,
                new Territory("BUR"), null, null);

        var orders = List.of(moveOrder, supportOrder, holdOrder);
        var units = List.of(attacker, supporter, defender);

        ResolutionResult result = resolver.resolve(orders, units, map);
        assertFalse(result.dislodgementResult().dislodgedUnits().isEmpty());
        assertTrue(result.dislodgementResult().dislodgedUnits().containsKey(defender));

        List<Territory> options = result.dislodgementResult().dislodgedUnits().get(defender);
        assertTrue(options.contains(new Territory("BEL")));
        assertTrue(options.contains(new Territory("PAR")));
        assertTrue(options.contains(new Territory("GAS")));
        assertTrue(options.contains(new Territory("BOH")));
        assertTrue(options.contains(new Territory("GAL")));
        assertEquals(5, options.size());
        assertFalse(options.contains(new Territory("MUN")));
        assertFalse(options.contains(new Territory("RUH")));
    }

    @Test
    void supportedDefense_repelsAttack() {
        var attacker = new Unit(UnitType.ARMY, germany, new Territory("MUN"));
        var defender = new Unit(UnitType.ARMY, france, new Territory("BUR"));
        var supporter = new Unit(UnitType.ARMY, france, new Territory("PAR"));

        Order moveOrder = new Order(OrderType.MOVE, attacker,
                new Territory("MUN"), new Territory("BUR"), null);
        Order holdOrder = new Order(OrderType.HOLD, defender,
                new Territory("BUR"), null, null);
        Order supportOrder = new Order(OrderType.SUPPORT, supporter,
                new Territory("PAR"), new Territory("BUR"), new Territory("BUR"));

        var orders = List.of(moveOrder, holdOrder, supportOrder);
        var units = List.of(attacker, defender, supporter);

        ResolutionResult result = resolver.resolve(orders, units, map);
        assertTrue(result.dislodgementResult().dislodgedUnits().isEmpty());
        assertTrue(result.dislodgementResult().contestedProvinces().isEmpty());
    }

    @Test
    void contestedProvince_neitherMoveSucceeds() {
        var attacker1 = new Unit(UnitType.ARMY, germany, new Territory("MUN"));
        var attacker2 = new Unit(UnitType.ARMY, france, new Territory("PAR"));

        Order move1 = new Order(OrderType.MOVE, attacker1,
                new Territory("MUN"), new Territory("BUR"), null);
        Order move2 = new Order(OrderType.MOVE, attacker2,
                new Territory("PAR"), new Territory("BUR"), null);

        var orders = List.of(move1, move2);
        var units = List.of(attacker1, attacker2);

        ResolutionResult result = resolver.resolve(orders, units, map);
        assertTrue(result.dislodgementResult().dislodgedUnits().isEmpty());
        assertEquals(1, result.dislodgementResult().contestedProvinces().size());
        assertTrue(result.dislodgementResult().contestedProvinces().contains("BUR"));
    }

    @Test
    void invalidMoveToContestedProvince_doesNotBlockValidMove() {
        var valid = new Unit(UnitType.ARMY, austria, new Territory("VIE"));
        var invalid = new Unit(UnitType.ARMY, russia, new Territory("MOS"));

        Order validMove = new Order(OrderType.MOVE, valid,
                new Territory("VIE"), new Territory("BOH"), null);
        Order invalidMove = new Order(OrderType.MOVE, invalid,
                new Territory("MOS"), new Territory("BOH"), null);

        var orders = List.of(validMove, invalidMove);
        var units = List.of(valid, invalid);

        ResolutionResult result = resolver.resolve(orders, units, map);
        assertTrue(result.dislodgementResult().contestedProvinces().isEmpty());
        assertEquals(OrderResult.FAILURE, result.orderResults().get(invalidMove));
        assertEquals(OrderResult.SUCCESS, result.orderResults().get(validMove));
    }

    @Test
    void fleetCannotMoveViaLandAdjacency() {
        var fleet = new Unit(UnitType.FLEET, austria, new Territory("BUL"));
        var army = new Unit(UnitType.ARMY, austria, new Territory("BUL"));

        Order fleetMove = new Order(OrderType.MOVE, fleet,
                new Territory("BUL"), new Territory("GRE"), null);
        Order armyMove = new Order(OrderType.MOVE, army,
                new Territory("BUL"), new Territory("GRE"), null);

        var fleetResult = resolver.resolve(List.of(fleetMove), List.of(fleet), map);
        assertEquals(OrderResult.FAILURE, fleetResult.orderResults().get(fleetMove));

        var armyResult = resolver.resolve(List.of(armyMove), List.of(army), map);
        assertEquals(OrderResult.SUCCESS, armyResult.orderResults().get(armyMove));
    }

    @Test
    void fleetCanMoveViaCoastalAdjacency() {
        var fleet = new Unit(UnitType.FLEET, turkey, new Territory("CON"));

        Order fleetMove = new Order(OrderType.MOVE, fleet,
                new Territory("CON"), new Territory("BUL"), null);

        var result = resolver.resolve(List.of(fleetMove), List.of(fleet), map);
        assertEquals(OrderResult.SUCCESS, result.orderResults().get(fleetMove));
    }

    @Test
    void unitMovingFromProvince_excludedFromRetreatOptions() {
        var attacker = new Unit(UnitType.ARMY, germany, new Territory("RUH"));
        var defender = new Unit(UnitType.ARMY, france, new Territory("BUR"));
        var supporter = new Unit(UnitType.ARMY, germany, new Territory("MUN"));

        Order moveOrder = new Order(OrderType.MOVE, attacker,
                new Territory("RUH"), new Territory("BUR"), null);
        Order supportOrder = new Order(OrderType.SUPPORT, supporter,
                new Territory("MUN"), new Territory("BUR"), new Territory("RUH"));
        Order holdOrder = new Order(OrderType.HOLD, defender,
                new Territory("BUR"), null, null);

        List<Territory> options = resolver.resolve(
                List.of(moveOrder, supportOrder, holdOrder),
                List.of(attacker, defender, supporter), map)
                .dislodgementResult().dislodgedUnits().get(defender);

        assertNotNull(options);
        assertFalse(options.contains(new Territory("RUH")));
    }

    @Test
    void fleetMovesFromSeaToSingleCoastCoastal() {
        var fleet = new Unit(UnitType.FLEET, france, new Territory("NTH"));
        Order move = new Order(OrderType.MOVE, fleet,
                new Territory("NTH"), new Territory("LON"), null);
        var result = resolver.resolve(List.of(move), List.of(fleet), map);
        assertEquals(OrderResult.SUCCESS, result.orderResults().get(move));
    }

    @Test
    void fleetMovesFromSingleCoastCoastalToSea() {
        var fleet = new Unit(UnitType.FLEET, france, new Territory("LON"));
        Order move = new Order(OrderType.MOVE, fleet,
                new Territory("LON"), new Territory("NTH"), null);
        var result = resolver.resolve(List.of(move), List.of(fleet), map);
        assertEquals(OrderResult.SUCCESS, result.orderResults().get(move));
    }

    @Test
    void fleetMovesBetweenSeaProvinces() {
        var fleet = new Unit(UnitType.FLEET, france, new Territory("NTH"));
        Order move = new Order(OrderType.MOVE, fleet,
                new Territory("NTH"), new Territory("ENG"), null);
        var result = resolver.resolve(List.of(move), List.of(fleet), map);
        assertEquals(OrderResult.SUCCESS, result.orderResults().get(move));
    }

    @Test
    void fleetMovesBetweenSingleCoastCoastalProvincesWithSharedSea() {
        var fleet = new Unit(UnitType.FLEET, france, new Territory("LON"));
        Order move = new Order(OrderType.MOVE, fleet,
                new Territory("LON"), new Territory("WAL"), null);
        var result = resolver.resolve(List.of(move), List.of(fleet), map);
        assertEquals(OrderResult.SUCCESS, result.orderResults().get(move));
    }

    @Test
    void fleetCannotMoveBetweenSingleCoastCoastalProvincesWithoutSharedSea() {
        var fleet = new Unit(UnitType.FLEET, austria, new Territory("ROM"));
        Order move = new Order(OrderType.MOVE, fleet,
                new Territory("ROM"), new Territory("VEN"), null);
        var result = resolver.resolve(List.of(move), List.of(fleet), map);
        assertEquals(OrderResult.FAILURE, result.orderResults().get(move));
    }

    @Test
    void fleetMovesFromNapToRom() {
        var fleet = new Unit(UnitType.FLEET, austria, new Territory("NAP"));
        Order move = new Order(OrderType.MOVE, fleet,
                new Territory("NAP"), new Territory("ROM"), null);
        var result = resolver.resolve(List.of(move), List.of(fleet), map);
        assertEquals(OrderResult.SUCCESS, result.orderResults().get(move));
    }

    @Test
    void armyMovesBetweenSingleCoastCoastalProvincesWithSharedSea() {
        var army = new Unit(UnitType.ARMY, france, new Territory("LON"));
        Order move = new Order(OrderType.MOVE, army,
                new Territory("LON"), new Territory("WAL"), null);
        var result = resolver.resolve(List.of(move), List.of(army), map);
        assertEquals(OrderResult.SUCCESS, result.orderResults().get(move));
    }

    @Test
    void fleetEntersProvinceWhoseUnitIsMovingAway() {
        var romeFleet = new Unit(UnitType.FLEET, austria, new Territory("ROM"));
        var tusFleet = new Unit(UnitType.FLEET, austria, new Territory("TUS"));
        Order romMove = new Order(OrderType.MOVE, romeFleet,
                new Territory("ROM"), new Territory("NAP"), null);
        Order tusMove = new Order(OrderType.MOVE, tusFleet,
                new Territory("TUS"), new Territory("ROM"), null);
        var result = resolver.resolve(List.of(romMove, tusMove), List.of(romeFleet, tusFleet), map);
        assertEquals(OrderResult.SUCCESS, result.orderResults().get(tusMove),
                "TUS fleet should enter ROM because ROM's unit is moving away");
    }
}
