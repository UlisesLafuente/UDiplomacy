package com.ulises.udiplomacy.domain.game.events;

import com.ulises.udiplomacy.domain.game.Nation;

import java.time.Instant;
import java.util.Set;

public record GameStarted(String gameId, Set<Nation> nations, Instant occurredAt) implements DomainEvent {
    public GameStarted(String gameId, Set<Nation> nations) {
        this(gameId, nations, Instant.now());
    }
}
