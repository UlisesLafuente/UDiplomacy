package com.ulises.udiplomacy.infrastructure.persistence.mongodb;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface SpringDataMongoMapVariantRepository extends MongoRepository<MongoMapVariantEntity, String> {
}
