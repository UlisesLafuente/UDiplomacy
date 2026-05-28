package com.ulises.udiplomacy.application.service;

import com.ulises.udiplomacy.application.port.output.EventPublisher;
import com.ulises.udiplomacy.application.port.output.GameProjectionRepository;
import com.ulises.udiplomacy.application.port.output.GameRepository;
import com.ulises.udiplomacy.domain.game.*;
import com.ulises.udiplomacy.domain.game.enums.UnitType;
import com.ulises.udiplomacy.domain.game.services.ConflictResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExecuteOrdersServiceTest {

    @Mock private GameRepository gameRepository;
    @Mock private ConflictResolver conflictResolver;
    @Mock private EventPublisher eventPublisher;
    @Mock private GameProjectionRepository projectionRepository;
    @InjectMocks private ExecuteOrdersService service;

    @Test
    void executesOrdersAndPublishesEvents() {
        GameMap map = TestMapLoader.loadClassic();
        Nation england = new Nation("ENGLAND");
        Unit unit = new Unit(UnitType.ARMY, england, new Territory("LON"));
        Game game = new Game("game-1", map, Set.of(england));
        game.start(List.of(unit));

        when(gameRepository.findById("game-1")).thenReturn(Optional.of(game));
        when(conflictResolver.resolve(any(), any(), any()))
                .thenReturn(new ResolutionResult(new DislodgementResult(Map.of(), Set.of()), Map.of()));

        ResolutionResult result = service.execute("game-1");

        assertNotNull(result);
        verify(eventPublisher, atLeastOnce()).publish(any());
        verify(gameRepository).save(game);
        assertTrue(game.events().isEmpty());
    }

    @Test
    void rejectsUnknownGame() {
        when(gameRepository.findById("unknown")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> service.execute("unknown"));
    }
}
