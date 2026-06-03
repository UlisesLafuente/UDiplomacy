package com.ulises.udiplomacy.infrastructure.persistence.postgres.projection;

import com.ulises.udiplomacy.application.port.output.GameProjectionRepository;
import com.ulises.udiplomacy.domain.game.Nation;
import com.ulises.udiplomacy.domain.user.GameReference;
import com.ulises.udiplomacy.domain.game.enums.GameState;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class GameProjectionRepositoryAdapter implements GameProjectionRepository {
    private final SpringDataJpaGameProjectionRepository springRepository;

    public GameProjectionRepositoryAdapter(SpringDataJpaGameProjectionRepository springRepository) {
        this.springRepository = springRepository;
    }

    @Override
    public void saveGameReference(GameReference reference) {
        var entity = new GameProjectionEntity();
        entity.setGameId(reference.gameId());
        entity.setUserId(reference.userId());
        entity.setUsername(reference.username());
        entity.setGameName(reference.gameName());
        entity.setStatus(reference.status().name());
        entity.setCreatedAt(reference.createdAt());
        entity.setUpdatedAt(reference.updatedAt());
        springRepository.save(entity);
    }

    @Override
    public void updateGameStatus(String gameId, String status) {
        springRepository.findById(gameId).ifPresent(entity -> {
            entity.setStatus(status);
            entity.setUpdatedAt(java.time.Instant.now());
            springRepository.save(entity);
        });
    }

    @Override
    public void saveFinalScores(String gameId, Nation winner, Map<Nation, Integer> scores) {
        springRepository.findById(gameId).ifPresent(entity -> {
            entity.setWinner(winner != null ? winner.name() : null);
            entity.setScores(scores.toString());
            entity.setStatus(GameState.FINISHED.name());
            entity.setUpdatedAt(java.time.Instant.now());
            springRepository.save(entity);
        });
    }

    @Override
    public List<GameReference> findByUserId(String userId) {
        return springRepository.findByUserId(userId).stream()
                .map(e -> new GameReference(e.getGameId(), e.getUserId(), e.getUsername(), e.getGameName(),
                        GameState.valueOf(e.getStatus()),
                        e.getCreatedAt(), e.getUpdatedAt()))
                .toList();
    }

    @Override
    public List<GameReference> findAll() {
        return springRepository.findAll().stream()
                .map(e -> new GameReference(e.getGameId(), e.getUserId(), e.getUsername(), e.getGameName(),
                        GameState.valueOf(e.getStatus()),
                        e.getCreatedAt(), e.getUpdatedAt()))
                .toList();
    }

    @Override
    public Optional<GameReference> findByGameId(String gameId) {
        return springRepository.findById(gameId)
                .map(e -> new GameReference(e.getGameId(), e.getUserId(), e.getUsername(), e.getGameName(),
                        GameState.valueOf(e.getStatus()),
                        e.getCreatedAt(), e.getUpdatedAt()));
    }

    @Override
    public void deleteByGameId(String gameId) {
        springRepository.deleteById(gameId);
    }
}
