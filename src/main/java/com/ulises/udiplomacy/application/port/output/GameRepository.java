package com.ulises.udiplomacy.application.port.output;

import com.ulises.udiplomacy.domain.game.Game;

import java.util.Optional;

public interface GameRepository {
    void save(Game game);
    Optional<Game> findById(String gameId);
    void deleteById(String gameId);
    boolean existsById(String gameId);
}
