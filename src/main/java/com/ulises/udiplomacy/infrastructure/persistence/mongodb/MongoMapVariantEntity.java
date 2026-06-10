package com.ulises.udiplomacy.infrastructure.persistence.mongodb;

import com.ulises.udiplomacy.domain.game.MapVariant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "map_variants")
public class MongoMapVariantEntity {
    @Id
    private String id;
    private String name;
    private String mapJson;
    private String svgContent;
    private boolean colonialRule;
    private Instant createdAt;

    public MongoMapVariantEntity() {}

    public MongoMapVariantEntity(MapVariant variant) {
        this.id = variant.id();
        this.name = variant.name();
        this.mapJson = variant.mapJson();
        this.svgContent = variant.svgContent();
        this.colonialRule = variant.colonialRule();
        this.createdAt = variant.createdAt();
    }

    public MapVariant toDomain() {
        return new MapVariant(id, name, mapJson, svgContent, colonialRule, createdAt);
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getMapJson() { return mapJson; }
    public void setMapJson(String mapJson) { this.mapJson = mapJson; }
    public String getSvgContent() { return svgContent; }
    public void setSvgContent(String svgContent) { this.svgContent = svgContent; }
    public boolean isColonialRule() { return colonialRule; }
    public void setColonialRule(boolean colonialRule) { this.colonialRule = colonialRule; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
