package com.ulises.udiplomacy.application.port.output;

import com.ulises.udiplomacy.domain.game.Nation;
import com.ulises.udiplomacy.domain.user.GameReference;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface GameProjectionRepository {
    void saveGameReference(GameReference reference);
    void updateGameStatus(String gameId, String status);
    void saveFinalScores(String gameId, Nation winner, Map<Nation, Integer> scores);
    List<GameReference> findByUserId(String userId);
    Optional<GameReference> findByGameId(String gameId);
    void deleteByGameId(String gameId);
}
