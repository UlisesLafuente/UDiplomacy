package com.ulises.udiplomacy.application.port.output;

import com.ulises.udiplomacy.domain.game.events.DomainEvent;

public interface EventPublisher {
    void publish(DomainEvent event);
}
