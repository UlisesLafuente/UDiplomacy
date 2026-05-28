package com.ulises.udiplomacy.infrastructure.persistence.mongodb;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface SpringDataMongoGameRepository extends MongoRepository<MongoGameEntity, String> {
}
