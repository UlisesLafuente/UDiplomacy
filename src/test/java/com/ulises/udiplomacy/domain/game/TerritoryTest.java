package com.ulises.udiplomacy.domain.game;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TerritoryTest {

    @Test
    void createsTerritoryWithProvinceOnly() {
        Territory t = new Territory("LON");
        assertEquals("LON", t.provinceName());
        assertTrue(t.coast().isEmpty());
    }

    @Test
    void createsTerritoryWithProvinceAndCoast() {
        Territory t = new Territory("SPA", new Coast("south"));
        assertEquals("SPA", t.provinceName());
        assertTrue(t.coast().isPresent());
        assertEquals("south", t.coast().get().name());
    }

    @Test
    void rejectsBlankProvince() {
        assertThrows(IllegalArgumentException.class, () -> new Territory(""));
        assertThrows(IllegalArgumentException.class, () -> new Territory("  "));
        assertThrows(IllegalArgumentException.class, () -> new Territory(null));
    }

    @Test
    void equality() {
        assertEquals(new Territory("LON"), new Territory("LON"));
        assertEquals(new Territory("SPA", new Coast("south")), new Territory("SPA", new Coast("south")));
        assertNotEquals(new Territory("LON"), new Territory("EDI"));
        assertNotEquals(new Territory("SPA", new Coast("north")), new Territory("SPA", new Coast("south")));
    }
}
