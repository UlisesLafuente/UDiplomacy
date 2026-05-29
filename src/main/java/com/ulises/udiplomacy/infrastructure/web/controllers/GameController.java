package com.ulises.udiplomacy.infrastructure.web.controllers;

import com.ulises.udiplomacy.application.port.input.*;
import com.ulises.udiplomacy.domain.game.*;
import com.ulises.udiplomacy.domain.user.GameReference;
import com.ulises.udiplomacy.infrastructure.web.dto.request.CreateGameRequest;
import com.ulises.udiplomacy.infrastructure.web.dto.request.SubmitOrderRequest;
import com.ulises.udiplomacy.infrastructure.web.dto.response.GameReferenceResponse;
import com.ulises.udiplomacy.infrastructure.web.dto.response.GameResponse;
import com.ulises.udiplomacy.infrastructure.web.dto.response.RetreatOptionsResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/games")
public class GameController {
    private final CreateGameUseCase createGameUseCase;
    private final GetGameUseCase getGameUseCase;
    private final SubmitOrderUseCase submitOrderUseCase;
    private final ExecuteOrdersUseCase executeOrdersUseCase;
    private final SaveGameUseCase saveGameUseCase;
    private final DeleteGameUseCase deleteGameUseCase;
    private final ListUserGamesUseCase listUserGamesUseCase;
    private final GetPendingDislodgedUnitsUseCase getPendingDislodgedUnitsUseCase;
    private final GetBuildOptionsUseCase getBuildOptionsUseCase;
    private final ResolveRetreatsUseCase resolveRetreatsUseCase;
    private final ResolveBuildsUseCase resolveBuildsUseCase;
    private final UndoLastTurnUseCase undoLastTurnUseCase;
    private final AdvancePhaseUseCase advancePhaseUseCase;
    private final RemoveOrderUseCase removeOrderUseCase;
    private final RewindGameUseCase rewindGameUseCase;
    private final GetGameHistoryUseCase getGameHistoryUseCase;
    private final GetOrderSyntaxUseCase getOrderSyntaxUseCase;

    public GameController(CreateGameUseCase createGameUseCase,
                           GetGameUseCase getGameUseCase,
                           SubmitOrderUseCase submitOrderUseCase,
                           ExecuteOrdersUseCase executeOrdersUseCase,
                           SaveGameUseCase saveGameUseCase,
                           DeleteGameUseCase deleteGameUseCase,
                           ListUserGamesUseCase listUserGamesUseCase,
                           GetPendingDislodgedUnitsUseCase getPendingDislodgedUnitsUseCase,
                           GetBuildOptionsUseCase getBuildOptionsUseCase,
                           ResolveRetreatsUseCase resolveRetreatsUseCase,
                           ResolveBuildsUseCase resolveBuildsUseCase,
                           UndoLastTurnUseCase undoLastTurnUseCase,
                           AdvancePhaseUseCase advancePhaseUseCase,
                           RemoveOrderUseCase removeOrderUseCase,
                           RewindGameUseCase rewindGameUseCase,
                           GetGameHistoryUseCase getGameHistoryUseCase,
                           GetOrderSyntaxUseCase getOrderSyntaxUseCase) {
        this.createGameUseCase = createGameUseCase;
        this.getGameUseCase = getGameUseCase;
        this.submitOrderUseCase = submitOrderUseCase;
        this.executeOrdersUseCase = executeOrdersUseCase;
        this.saveGameUseCase = saveGameUseCase;
        this.deleteGameUseCase = deleteGameUseCase;
        this.listUserGamesUseCase = listUserGamesUseCase;
        this.getPendingDislodgedUnitsUseCase = getPendingDislodgedUnitsUseCase;
        this.getBuildOptionsUseCase = getBuildOptionsUseCase;
        this.resolveRetreatsUseCase = resolveRetreatsUseCase;
        this.resolveBuildsUseCase = resolveBuildsUseCase;
        this.undoLastTurnUseCase = undoLastTurnUseCase;
        this.advancePhaseUseCase = advancePhaseUseCase;
        this.removeOrderUseCase = removeOrderUseCase;
        this.rewindGameUseCase = rewindGameUseCase;
        this.getGameHistoryUseCase = getGameHistoryUseCase;
        this.getOrderSyntaxUseCase = getOrderSyntaxUseCase;
    }

    @PostMapping
    public ResponseEntity<GameResponse> createGame(@Valid @RequestBody CreateGameRequest request,
                                                    Authentication auth) {
        Game game = createGameUseCase.execute(request.mapId(), request.mapJson(), auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(GameResponse.from(game));
    }

    @GetMapping
    public ResponseEntity<List<GameReferenceResponse>> listGames(Authentication auth) {
        List<GameReference> refs = listUserGamesUseCase.execute(auth.getName());
        return ResponseEntity.ok(refs.stream().map(GameReferenceResponse::from).toList());
    }

    @GetMapping("/{gameId}")
    public ResponseEntity<GameResponse> getGame(@PathVariable String gameId) {
        Game game = getGameUseCase.execute(gameId);
        return ResponseEntity.ok(GameResponse.from(game));
    }

    @PostMapping("/{gameId}/orders")
    public ResponseEntity<Void> submitOrder(@PathVariable String gameId,
                                             @Valid @RequestBody SubmitOrderRequest request) {
        submitOrderUseCase.execute(gameId, request.rawOrder());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{gameId}/orders/{index}")
    public ResponseEntity<Void> removeOrder(@PathVariable String gameId,
                                             @PathVariable int index) {
        removeOrderUseCase.execute(gameId, index);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{gameId}/execute")
    public ResponseEntity<Void> executeOrders(@PathVariable String gameId) {
        executeOrdersUseCase.execute(gameId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{gameId}/retreats")
    public ResponseEntity<Void> resolveRetreats(@PathVariable String gameId,
                                                  @Valid @RequestBody List<SubmitOrderRequest> orders) {
        resolveRetreatsUseCase.execute(gameId,
                orders.stream().map(SubmitOrderRequest::rawOrder).toList());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{gameId}/builds")
    public ResponseEntity<Void> resolveBuilds(@PathVariable String gameId,
                                                @Valid @RequestBody List<SubmitOrderRequest> orders) {
        resolveBuildsUseCase.execute(gameId,
                orders.stream().map(SubmitOrderRequest::rawOrder).toList());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{gameId}/retreat-options")
    public ResponseEntity<RetreatOptionsResponse> getRetreatOptions(@PathVariable String gameId) {
        DislodgementResult result = getPendingDislodgedUnitsUseCase.execute(gameId);
        var units = result.dislodgedUnits().entrySet().stream()
                .map(e -> new RetreatOptionsResponse.DislodgedUnitOptions(
                        e.getKey().unitType().name(),
                        e.getKey().nation().name(),
                        e.getKey().location().provinceName(),
                        e.getValue().stream().map(Territory::provinceName).toList()
                ))
                .toList();
        return ResponseEntity.ok(new RetreatOptionsResponse(units));
    }

    @GetMapping("/{gameId}/build-options")
    public ResponseEntity<List<BuildCapacity>> getBuildOptions(@PathVariable String gameId) {
        Game game = getGameUseCase.execute(gameId);
        var capacities = game.nations().stream()
                .map(game::getBuildOptions)
                .toList();
        return ResponseEntity.ok(capacities);
    }

    @PostMapping("/{gameId}/save")
    public ResponseEntity<Void> saveGame(@PathVariable String gameId, Authentication auth) {
        saveGameUseCase.execute(gameId, auth.getName());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{gameId}")
    public ResponseEntity<Void> deleteGame(@PathVariable String gameId) {
        deleteGameUseCase.execute(gameId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{gameId}/undo")
    public ResponseEntity<Void> undoLastTurn(@PathVariable String gameId) {
        undoLastTurnUseCase.execute(gameId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{gameId}/advance")
    public ResponseEntity<Void> advancePhase(@PathVariable String gameId) {
        advancePhaseUseCase.execute(gameId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{gameId}/rewind/{turnIndex}")
    public ResponseEntity<Void> rewindToTurn(@PathVariable String gameId,
                                              @PathVariable int turnIndex) {
        rewindGameUseCase.execute(gameId, turnIndex);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{gameId}/history")
    public ResponseEntity<GameResponse> getHistory(@PathVariable String gameId) {
        Game game = getGameHistoryUseCase.execute(gameId);
        return ResponseEntity.ok(GameResponse.from(game));
    }
}
