package com.ulises.udiplomacy.domain.game.events;

import java.time.Instant;

public interface DomainEvent {
    Instant occurredAt();
}
