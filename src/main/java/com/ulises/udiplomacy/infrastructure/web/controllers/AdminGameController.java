package com.ulises.udiplomacy.infrastructure.web.controllers;

import com.ulises.udiplomacy.application.port.input.DeleteGameUseCase;
import com.ulises.udiplomacy.application.port.input.ListAllGamesUseCase;
import com.ulises.udiplomacy.infrastructure.web.dto.response.GameReferenceResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/games")
public class AdminGameController {
    private final DeleteGameUseCase deleteGameUseCase;
    private final ListAllGamesUseCase listAllGamesUseCase;

    public AdminGameController(DeleteGameUseCase deleteGameUseCase,
                                ListAllGamesUseCase listAllGamesUseCase) {
        this.deleteGameUseCase = deleteGameUseCase;
        this.listAllGamesUseCase = listAllGamesUseCase;
    }

    @GetMapping
    public ResponseEntity<List<GameReferenceResponse>> listAllGames() {
        var refs = listAllGamesUseCase.execute();
        return ResponseEntity.ok(refs.stream().map(GameReferenceResponse::from).toList());
    }

    @DeleteMapping("/{gameId}")
    public ResponseEntity<Void> deleteGame(@PathVariable String gameId) {
        deleteGameUseCase.execute(gameId);
        return ResponseEntity.noContent().build();
    }
}
