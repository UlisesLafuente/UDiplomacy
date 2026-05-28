package com.ulises.udiplomacy.infrastructure.web.controllers;

import com.ulises.udiplomacy.application.port.input.*;
import com.ulises.udiplomacy.domain.game.*;
import com.ulises.udiplomacy.domain.game.enums.UnitType;
import com.ulises.udiplomacy.infrastructure.web.dto.request.CreateGameRequest;
import com.ulises.udiplomacy.infrastructure.web.dto.request.SubmitOrderRequest;
import com.ulises.udiplomacy.infrastructure.web.dto.response.GameResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GameControllerTest {

    private final Authentication auth = new UsernamePasswordAuthenticationToken("user-1", null);
    private final GameController controller;
    private final Game game;

    GameControllerTest() {
        CreateGameUseCase createGameUseCase = mock(CreateGameUseCase.class);
        GetGameUseCase getGameUseCase = mock(GetGameUseCase.class);
        SubmitOrderUseCase submitOrderUseCase = mock(SubmitOrderUseCase.class);
        ExecuteOrdersUseCase executeOrdersUseCase = mock(ExecuteOrdersUseCase.class);
        SaveGameUseCase saveGameUseCase = mock(SaveGameUseCase.class);
        DeleteGameUseCase deleteGameUseCase = mock(DeleteGameUseCase.class);
        ListUserGamesUseCase listUserGamesUseCase = mock(ListUserGamesUseCase.class);
        GetPendingDislodgedUnitsUseCase getPendingDislodgedUnitsUseCase = mock(GetPendingDislodgedUnitsUseCase.class);
        GetBuildOptionsUseCase getBuildOptionsUseCase = mock(GetBuildOptionsUseCase.class);
        ResolveRetreatsUseCase resolveRetreatsUseCase = mock(ResolveRetreatsUseCase.class);
        ResolveBuildsUseCase resolveBuildsUseCase = mock(ResolveBuildsUseCase.class);
        UndoLastTurnUseCase undoLastTurnUseCase = mock(UndoLastTurnUseCase.class);
        AdvancePhaseUseCase advancePhaseUseCase = mock(AdvancePhaseUseCase.class);
        RemoveOrderUseCase removeOrderUseCase = mock(RemoveOrderUseCase.class);
        RewindGameUseCase rewindGameUseCase = mock(RewindGameUseCase.class);
        GetGameHistoryUseCase getGameHistoryUseCase = mock(GetGameHistoryUseCase.class);
        GetOrderSyntaxUseCase getOrderSyntaxUseCase = mock(GetOrderSyntaxUseCase.class);

        controller = new GameController(
                createGameUseCase, getGameUseCase, submitOrderUseCase,
                executeOrdersUseCase, saveGameUseCase, deleteGameUseCase,
                listUserGamesUseCase, getPendingDislodgedUnitsUseCase, getBuildOptionsUseCase,
                resolveRetreatsUseCase, resolveBuildsUseCase,
                undoLastTurnUseCase, advancePhaseUseCase,
                removeOrderUseCase, rewindGameUseCase,
                getGameHistoryUseCase, getOrderSyntaxUseCase);

        GameMap map = TestMapLoader.loadClassic();
        game = new Game("game-1", map, Set.of(new Nation("ENGLAND")));
        game.start(List.of(
                new Unit(UnitType.ARMY, new Nation("ENGLAND"), new Territory("LON"))
        ));

        when(createGameUseCase.execute(any(), any(), any())).thenReturn(game);
        when(getGameUseCase.execute("game-1")).thenReturn(game);
    }

    @Test
    void createGame_returns201() {
        ResponseEntity<GameResponse> response = controller.createGame(
                new CreateGameRequest(null, "{}"), auth);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("game-1", response.getBody().gameId());
    }

    @Test
    void getGame_returnsGame() {
        ResponseEntity<GameResponse> response = controller.getGame("game-1");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("game-1", response.getBody().gameId());
    }

    @Test
    void submitOrder_returns200() {
        ResponseEntity<Void> response = controller.submitOrder("game-1",
                new SubmitOrderRequest("A LON H"));
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void executeOrders_returns200() {
        ResponseEntity<Void> response = controller.executeOrders("game-1");
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void deleteGame_returns204() {
        ResponseEntity<Void> response = controller.deleteGame("game-1");
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }
}
