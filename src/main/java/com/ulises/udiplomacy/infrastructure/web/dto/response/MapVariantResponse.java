package com.ulises.udiplomacy.infrastructure.web.dto.response;

import com.ulises.udiplomacy.domain.game.MapVariant;

public record MapVariantResponse(
        String id,
        String name,
        String svgContent,
        boolean colonialRule,
        String createdAt
) {
    public static MapVariantResponse summary(MapVariant v) {
        return new MapVariantResponse(v.id(), v.name(), null, v.colonialRule(), v.createdAt().toString());
    }

    public static MapVariantResponse full(MapVariant v) {
        return new MapVariantResponse(v.id(), v.name(), v.svgContent(), v.colonialRule(), v.createdAt().toString());
    }
}
