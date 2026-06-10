package com.ulises.udiplomacy.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ulises.udiplomacy.application.port.output.GameProjectionRepository;
import com.ulises.udiplomacy.application.port.output.GameRepository;
import com.ulises.udiplomacy.application.port.output.MapVariantRepository;
import com.ulises.udiplomacy.application.port.output.UserRepository;
import com.ulises.udiplomacy.domain.game.*;
import com.ulises.udiplomacy.domain.game.enums.UnitType;
import com.ulises.udiplomacy.domain.user.Role;
import com.ulises.udiplomacy.domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateGameServiceTest {

    @Mock private GameRepository gameRepository;
    @Mock private GameProjectionRepository projectionRepository;
    @Mock private MapVariantRepository mapVariantRepository;
    @Mock private UserRepository userRepository;
    private CreateGameService service;
    private String mapJson;

    @Captor private ArgumentCaptor<Game> gameCaptor;

    @BeforeEach
    void setUp() throws Exception {
        when(userRepository.findById("user-1"))
                .thenReturn(java.util.Optional.of(new User("user-1", "test", "", Role.PLAYER)));
        service = new CreateGameService(gameRepository, projectionRepository,
                mapVariantRepository, userRepository, new ObjectMapper());
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("europe-classic.json")) {
            mapJson = new String(is.readAllBytes());
        }
    }

    @Test
    void createsGameWithAllNations() {
        Game game = service.execute(null, mapJson, "user-1");

        assertNotNull(game.gameId());
        assertEquals(7, game.nations().size());
        assertTrue(game.nations().contains(new Nation("ENGLAND")));
        assertTrue(game.nations().contains(new Nation("FRANCE")));
        verify(gameRepository).save(gameCaptor.capture());
        verify(projectionRepository).saveGameReference(any());
    }

    @Test
    void createsGameWithInitialUnits() {
        Game game = service.execute(null, mapJson, "user-1");

        assertEquals(22, game.currentTurn().units().size());
        assertTrue(game.currentTurn().units().stream()
                .anyMatch(u -> u.unitType() == UnitType.FLEET
                        && u.nation().equals(new Nation("ENGLAND"))
                        && u.location().provinceName().equals("EDI")));
        assertTrue(game.currentTurn().units().stream()
                .anyMatch(u -> u.location().provinceName().equals("BRE")));
    }

    @Test
    void createsGameWithMapId() {
        when(mapVariantRepository.findById("europe-classic"))
                .thenReturn(java.util.Optional.of(
                        new MapVariant("europe-classic", "Classic", mapJson, null, false, null)));

        Game game = service.execute("europe-classic", null, "user-1");

        assertNotNull(game.gameId());
        assertEquals(7, game.nations().size());
        verify(mapVariantRepository).findById("europe-classic");
    }
}
