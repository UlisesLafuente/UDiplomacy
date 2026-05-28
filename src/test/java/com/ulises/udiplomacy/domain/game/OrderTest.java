package com.ulises.udiplomacy.domain.game;

import com.ulises.udiplomacy.domain.game.enums.OrderType;
import com.ulises.udiplomacy.domain.game.enums.UnitType;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OrderTest {

    private final Unit stubUnit = new Unit(UnitType.ARMY, new Nation("ENGLAND"), new Territory("LON"));

    @Test
    void createsHoldOrder() {
        Order o = new Order(OrderType.HOLD, stubUnit, new Territory("LON"), null, null);
        assertEquals(OrderType.HOLD, o.type());
        assertEquals(stubUnit, o.unit());
        assertEquals("LON", o.source().provinceName());
        assertTrue(o.target().isEmpty());
        assertTrue(o.auxiliary().isEmpty());
    }

    @Test
    void createsMoveOrder() {
        Order o = new Order(OrderType.MOVE, stubUnit, new Territory("LON"),
                new Territory("NTH"), null);
        assertEquals(OrderType.MOVE, o.type());
        assertTrue(o.target().isPresent());
        assertEquals("NTH", o.target().get().provinceName());
    }

    @Test
    void createsSupportOrder() {
        Order o = new Order(OrderType.SUPPORT, stubUnit, new Territory("LON"),
                new Territory("YOR"), new Territory("EDI"));
        assertTrue(o.target().isPresent());
        assertTrue(o.auxiliary().isPresent());
        assertEquals("YOR", o.target().get().provinceName());
        assertEquals("EDI", o.auxiliary().get().provinceName());
    }

    @Test
    void equality() {
        Order a = new Order(OrderType.HOLD, stubUnit, new Territory("LON"), null, null);
        Order b = new Order(OrderType.HOLD, stubUnit, new Territory("LON"), null, null);
        assertEquals(a, b);
    }
}
