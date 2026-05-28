package com.ulises.udiplomacy.domain.game.events;

import java.time.Instant;

public record TurnEnded(String gameId, int year, String season, Instant occurredAt) implements DomainEvent {
    public TurnEnded(String gameId, int year, String season) {
        this(gameId, year, season, Instant.now());
    }
}
