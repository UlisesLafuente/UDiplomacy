package com.ulises.udiplomacy.domain.game;

import java.time.Instant;
import java.util.Objects;

public final class MapVariant {
    private final String id;
    private final String name;
    private final String mapJson;
    private final String svgContent;
    private final boolean colonialRule;
    private final Instant createdAt;

    public MapVariant(String id, String name, String mapJson, String svgContent, boolean colonialRule, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.mapJson = mapJson;
        this.svgContent = svgContent;
        this.colonialRule = colonialRule;
        this.createdAt = createdAt;
    }

    public String id() { return id; }
    public String name() { return name; }
    public String mapJson() { return mapJson; }
    public String svgContent() { return svgContent; }
    public boolean colonialRule() { return colonialRule; }
    public Instant createdAt() { return createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MapVariant that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hashCode(id); }
}
