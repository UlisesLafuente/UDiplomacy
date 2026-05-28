package com.ulises.udiplomacy.infrastructure.persistence.postgres.projection;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataJpaGameProjectionRepository extends JpaRepository<GameProjectionEntity, String> {
    List<GameProjectionEntity> findByUserId(String userId);
}
