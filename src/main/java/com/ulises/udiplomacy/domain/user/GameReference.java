package com.ulises.udiplomacy.domain.user;

import com.ulises.udiplomacy.domain.game.enums.GameState;

import java.time.Instant;
import java.util.Objects;

public final class GameReference {
    private final String gameId;
    private final String userId;
    private final String username;
    private final String gameName;
    private final GameState status;
    private final Instant createdAt;
    private final Instant updatedAt;

    public GameReference(String gameId, String userId, String username, String gameName,
                         GameState status, Instant createdAt, Instant updatedAt) {
        this.gameId = gameId;
        this.userId = userId;
        this.username = username;
        this.gameName = gameName;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String gameId() { return gameId; }
    public String userId() { return userId; }
    public String username() { return username; }
    public String gameName() { return gameName; }
    public GameState status() { return status; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GameReference that)) return false;
        return Objects.equals(gameId, that.gameId);
    }

    @Override
    public int hashCode() { return Objects.hashCode(gameId); }
}
