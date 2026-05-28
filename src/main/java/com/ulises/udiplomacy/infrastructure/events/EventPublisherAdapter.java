package com.ulises.udiplomacy.infrastructure.events;

import com.ulises.udiplomacy.application.port.output.EventPublisher;
import com.ulises.udiplomacy.application.port.output.GameProjectionRepository;
import com.ulises.udiplomacy.domain.game.events.DomainEvent;
import com.ulises.udiplomacy.domain.game.events.GameFinished;
import com.ulises.udiplomacy.domain.game.events.GameSaved;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventPublisherAdapter implements EventPublisher {
    private static final Logger log = LoggerFactory.getLogger(EventPublisherAdapter.class);
    private final GameProjectionRepository projectionRepository;

    public EventPublisherAdapter(GameProjectionRepository projectionRepository) {
        this.projectionRepository = projectionRepository;
    }

    @Override
    public void publish(DomainEvent event) {
        switch (event) {
            case GameSaved saved -> {
                log.info("Game saved: {}", saved.gameId());
            }
            case GameFinished finished -> {
                log.info("Game finished: {} winner: {}", finished.gameId(), finished.winner());
                projectionRepository.saveFinalScores(
                        finished.gameId(), finished.winner(), finished.scores());
            }
            default -> log.debug("Event published: {}", event);
        }
    }
}
