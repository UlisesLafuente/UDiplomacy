package com.ulises.udiplomacy.application.service;

import com.ulises.udiplomacy.application.port.output.GameRepository;
import com.ulises.udiplomacy.domain.game.*;
import com.ulises.udiplomacy.domain.game.enums.OrderType;
import com.ulises.udiplomacy.domain.game.enums.UnitType;
import com.ulises.udiplomacy.domain.game.services.OrderParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubmitOrderServiceTest {

    @Mock private GameRepository gameRepository;
    @Mock private OrderParser orderParser;
    @InjectMocks private SubmitOrderService service;

    @Test
    void submitsOrder() {
        GameMap map = TestMapLoader.loadClassic();
        Unit unit = new Unit(UnitType.ARMY, new Nation("ENGLAND"), new Territory("LON"));
        Game game = new Game("game-1", map, Set.of(new Nation("ENGLAND")));
        game.start(java.util.List.of(unit));

        Order parsedOrder = new Order(OrderType.HOLD, unit, new Territory("LON"), null, null);

        when(gameRepository.findById("game-1")).thenReturn(Optional.of(game));
        when(orderParser.parse("A LON H", map)).thenReturn(parsedOrder);

        Order result = service.execute("game-1", "A LON H");

        assertEquals(OrderType.HOLD, result.type());
        assertEquals(1, game.orderPool().size());
        verify(gameRepository).save(game);
    }

    @Test
    void rejectsUnknownGame() {
        when(gameRepository.findById("unknown")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> service.execute("unknown", "A LON H"));
    }
}
