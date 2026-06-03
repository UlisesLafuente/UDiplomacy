package com.ulises.udiplomacy.infrastructure.web.dto.response;

import com.ulises.udiplomacy.domain.user.GameReference;

public record GameReferenceResponse(
        String gameId,
        String userId,
        String username,
        String gameName,
        String status,
        String createdAt
) {
    public static GameReferenceResponse from(GameReference ref) {
        return new GameReferenceResponse(
                ref.gameId(), ref.userId(), ref.username(), ref.gameName(),
                ref.status().name(), ref.createdAt().toString()
        );
    }
}
