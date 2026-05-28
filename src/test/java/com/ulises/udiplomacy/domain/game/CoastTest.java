package com.ulises.udiplomacy.domain.game;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CoastTest {

    @Test
    void createsCoast() {
        Coast c = new Coast("north");
        assertEquals("north", c.name());
    }

    @Test
    void rejectsBlankName() {
        assertThrows(IllegalArgumentException.class, () -> new Coast(""));
        assertThrows(IllegalArgumentException.class, () -> new Coast(null));
    }

    @Test
    void equality() {
        assertEquals(new Coast("north"), new Coast("north"));
        assertNotEquals(new Coast("north"), new Coast("south"));
    }
}
