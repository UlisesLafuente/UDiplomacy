package com.ulises.udiplomacy.domain.game.events;

import java.time.Instant;

public record GameSaved(String gameId, String userId, Instant occurredAt) implements DomainEvent {
    public GameSaved(String gameId, String userId) {
        this(gameId, userId, Instant.now());
    }
}
