package com.ulises.udiplomacy.domain.game.services;

import com.ulises.udiplomacy.domain.game.GameMap;
import com.ulises.udiplomacy.domain.game.Order;
import com.ulises.udiplomacy.domain.game.TestMapLoader;
import com.ulises.udiplomacy.domain.game.enums.OrderType;
import com.ulises.udiplomacy.domain.game.enums.UnitType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OrderParserTest {

    private OrderParser parser;
    private GameMap map;

    @BeforeEach
    void setUp() {
        parser = new OrderParser();
        map = TestMapLoader.loadClassic();
    }

    @Test
    void parsesArmyHold() {
        Order o = parser.parse("A LON H", map);
        assertEquals(OrderType.HOLD, o.type());
        assertEquals(UnitType.ARMY, o.unit().unitType());
        assertEquals("LON", o.source().provinceName());
    }

    @Test
    void parsesFleetHold() {
        Order o = parser.parse("F NTH H", map);
        assertEquals(OrderType.HOLD, o.type());
        assertEquals(UnitType.FLEET, o.unit().unitType());
        assertEquals("NTH", o.source().provinceName());
    }

    @Test
    void parsesMove() {
        Order o = parser.parse("A PAR - BUR", map);
        assertEquals(OrderType.MOVE, o.type());
        assertEquals("PAR", o.source().provinceName());
        assertTrue(o.target().isPresent());
        assertEquals("BUR", o.target().get().provinceName());
    }

    @Test
    void parsesSupportHold() {
        Order o = parser.parse("A PAR S BUR", map);
        assertEquals(OrderType.SUPPORT, o.type());
        assertEquals("PAR", o.source().provinceName());
        assertTrue(o.auxiliary().isPresent());
        assertEquals("BUR", o.auxiliary().get().provinceName());
        assertTrue(o.target().isEmpty());
    }

    @Test
    void parsesSupportMove() {
        Order o = parser.parse("A PAR S BRE GAS", map);
        assertEquals(OrderType.SUPPORT, o.type());
        assertEquals("BRE", o.auxiliary().get().provinceName());
        assertTrue(o.target().isPresent());
        assertEquals("GAS", o.target().get().provinceName());
    }

    @Test
    void parsesConvoy() {
        Order o = parser.parse("F NTH C LON HOL", map);
        assertEquals(OrderType.CONVOY, o.type());
        assertEquals(UnitType.FLEET, o.unit().unitType());
        assertEquals("LON", o.auxiliary().get().provinceName());
        assertEquals("HOL", o.target().get().provinceName());
    }

    @Test
    void parsesRetreat() {
        Order o = parser.parse("A PAR R GAS", map);
        assertEquals(OrderType.RETREAT, o.type());
        assertEquals("GAS", o.target().get().provinceName());
    }

    @Test
    void parsesBuild() {
        Order o = parser.parse("A LON B LON", map);
        assertEquals(OrderType.BUILD, o.type());
        assertEquals("LON", o.target().get().provinceName());
    }

    @Test
    void parsesDisband() {
        Order o = parser.parse("A PAR D", map);
        assertEquals(OrderType.DISBAND, o.type());
    }

    @Test
    void parsesFleetHoldInCoastal() {
        Order o = parser.parse("F BRE H", map);
        assertEquals(OrderType.HOLD, o.type());
        assertEquals(UnitType.FLEET, o.unit().unitType());
    }

    @Test
    void parsesFleetMoveToSea() {
        Order o = parser.parse("F LON - ENG", map);
        assertEquals(OrderType.MOVE, o.type());
        assertEquals("ENG", o.target().get().provinceName());
    }

    @Test
    void rejectsEmptyOrder() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse("", map));
        assertThrows(IllegalArgumentException.class, () -> parser.parse(null, map));
    }

    @Test
    void rejectsTooShortOrder() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse("A LON", map));
    }

    @Test
    void rejectsUnknownUnitType() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse("X LON H", map));
    }

    @Test
    void rejectsUnknownProvince() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse("A XYZ H", map));
    }

    @Test
    void rejectsArmyInSea() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse("A NTH H", map));
    }

    @Test
    void rejectsFleetInInland() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse("F PAR H", map));
    }

    @Test
    void rejectsArmyMoveToSea() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse("A LON - NTH", map));
    }

    @Test
    void rejectsFleetMoveToInland() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse("F LON - PAR", map));
    }

    @Test
    void rejectsNonAdjacentMove() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse("A LON - PAR", map));
    }

    @Test
    void rejectsMoveWithoutTarget() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse("A PAR -", map));
    }

    @Test
    void rejectsRetreatWithoutTarget() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse("A PAR R", map));
    }

    @Test
    void rejectsUnknownAction() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse("A PAR Z", map));
    }

    @Test
    void acceptsAbbreviatedUnitTypes() {
        assertEquals(UnitType.ARMY, parser.parse("A LON H", map).unit().unitType());
        assertEquals(UnitType.FLEET, parser.parse("F NTH H", map).unit().unitType());
    }

    @Test
    void acceptsFullUnitTypes() {
        assertEquals(UnitType.ARMY, parser.parse("ARMY LON H", map).unit().unitType());
        assertEquals(UnitType.FLEET, parser.parse("FLEET NTH H", map).unit().unitType());
    }
}
