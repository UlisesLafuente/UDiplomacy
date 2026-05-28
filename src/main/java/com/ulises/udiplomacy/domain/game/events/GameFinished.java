package com.ulises.udiplomacy.domain.game.events;

import com.ulises.udiplomacy.domain.game.Nation;

import java.time.Instant;
import java.util.Map;

public record GameFinished(String gameId, Nation winner, Map<Nation, Integer> scores, Instant occurredAt) implements DomainEvent {
    public GameFinished(String gameId, Nation winner, Map<Nation, Integer> scores) {
        this(gameId, winner, scores, Instant.now());
    }
}
