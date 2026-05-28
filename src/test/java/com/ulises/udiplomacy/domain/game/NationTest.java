package com.ulises.udiplomacy.domain.game;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NationTest {

    @Test
    void createsNation() {
        Nation n = new Nation("ENGLAND");
        assertEquals("ENGLAND", n.name());
    }

    @Test
    void rejectsBlankName() {
        assertThrows(IllegalArgumentException.class, () -> new Nation(""));
        assertThrows(IllegalArgumentException.class, () -> new Nation(null));
    }

    @Test
    void equality() {
        assertEquals(new Nation("ENGLAND"), new Nation("ENGLAND"));
        assertNotEquals(new Nation("ENGLAND"), new Nation("FRANCE"));
    }
}
