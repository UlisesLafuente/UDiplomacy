package com.ulises.udiplomacy.infrastructure.persistence.mongodb;

import com.ulises.udiplomacy.application.port.output.GameRepository;
import com.ulises.udiplomacy.domain.game.Game;

import java.util.Optional;

public class MongoGameRepositoryAdapter implements GameRepository {
    private final SpringDataMongoGameRepository springRepository;

    public MongoGameRepositoryAdapter(SpringDataMongoGameRepository springRepository) {
        this.springRepository = springRepository;
    }

    @Override
    public void save(Game game) {
        springRepository.save(new MongoGameEntity(game));
    }

    @Override
    public Optional<Game> findById(String gameId) {
        return springRepository.findById(gameId)
                .map(MongoGameEntity::toDomain);
    }

    @Override
    public void deleteById(String gameId) {
        springRepository.deleteById(gameId);
    }

    @Override
    public boolean existsById(String gameId) {
        return springRepository.existsById(gameId);
    }
}
