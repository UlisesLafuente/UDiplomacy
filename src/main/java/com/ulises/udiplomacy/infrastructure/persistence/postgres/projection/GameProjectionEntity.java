package com.ulises.udiplomacy.infrastructure.persistence.postgres.projection;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "game_projections")
public class GameProjectionEntity {
    @Id
    private String gameId;
    private String userId;
    private String gameName;
    private String status;
    private String winner;
    private String scores;
    private Instant createdAt;
    private Instant updatedAt;

    public GameProjectionEntity() {}

    public String getGameId() { return gameId; }
    public void setGameId(String gameId) { this.gameId = gameId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getGameName() { return gameName; }
    public void setGameName(String gameName) { this.gameName = gameName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getWinner() { return winner; }
    public void setWinner(String winner) { this.winner = winner; }
    public String getScores() { return scores; }
    public void setScores(String scores) { this.scores = scores; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
