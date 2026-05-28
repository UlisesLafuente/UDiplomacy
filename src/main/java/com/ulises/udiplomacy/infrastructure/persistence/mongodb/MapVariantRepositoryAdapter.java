package com.ulises.udiplomacy.infrastructure.persistence.mongodb;

import com.ulises.udiplomacy.application.port.output.MapVariantRepository;
import com.ulises.udiplomacy.domain.game.MapVariant;

import java.util.List;
import java.util.Optional;

public class MapVariantRepositoryAdapter implements MapVariantRepository {
    private final SpringDataMongoMapVariantRepository springRepository;

    public MapVariantRepositoryAdapter(SpringDataMongoMapVariantRepository springRepository) {
        this.springRepository = springRepository;
    }

    @Override
    public void save(MapVariant variant) {
        springRepository.save(new MongoMapVariantEntity(variant));
    }

    @Override
    public Optional<MapVariant> findById(String id) {
        return springRepository.findById(id).map(MongoMapVariantEntity::toDomain);
    }

    @Override
    public List<MapVariant> findAll() {
        return springRepository.findAll().stream().map(MongoMapVariantEntity::toDomain).toList();
    }

    @Override
    public boolean existsById(String id) {
        return springRepository.existsById(id);
    }

    @Override
    public void deleteById(String id) {
        springRepository.deleteById(id);
    }
}
