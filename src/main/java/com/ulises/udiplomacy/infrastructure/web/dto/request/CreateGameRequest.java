package com.ulises.udiplomacy.infrastructure.web.dto.request;

public record CreateGameRequest(
        String mapId,
        String mapJson
) {
    public CreateGameRequest {
        if (mapId == null && mapJson == null) {
            throw new IllegalArgumentException("Either mapId or mapJson is required");
        }
    }
}
