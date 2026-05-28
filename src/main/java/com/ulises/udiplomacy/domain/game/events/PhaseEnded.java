package com.ulises.udiplomacy.domain.game.events;

import com.ulises.udiplomacy.domain.game.DislodgementResult;

import java.time.Instant;

public record PhaseEnded(String gameId, String phase, DislodgementResult result, Instant occurredAt) implements DomainEvent {
    public PhaseEnded(String gameId, String phase, DislodgementResult result) {
        this(gameId, phase, result, Instant.now());
    }
}
