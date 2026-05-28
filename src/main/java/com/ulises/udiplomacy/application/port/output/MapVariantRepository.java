package com.ulises.udiplomacy.application.port.output;

import com.ulises.udiplomacy.domain.game.MapVariant;

import java.util.List;
import java.util.Optional;

public interface MapVariantRepository {
    void save(MapVariant variant);
    Optional<MapVariant> findById(String id);
    List<MapVariant> findAll();
    boolean existsById(String id);
    void deleteById(String id);
}
