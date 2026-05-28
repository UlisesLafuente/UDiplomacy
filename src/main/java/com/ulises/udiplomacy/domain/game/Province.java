package com.ulises.udiplomacy.domain.game;

import com.ulises.udiplomacy.domain.game.enums.ProvinceType;

import java.util.*;

public final class Province {
    private final String name;
    private final ProvinceType type;
    private final Nation homeNation;
    private final boolean supplyCenter;
    private final List<Coast> coasts;
    private final Map<String, Coast> adjacencies;

    public Province(String name, ProvinceType type, Nation homeNation, boolean supplyCenter,
                    List<Coast> coasts, Map<String, Coast> adjacencies) {
        this.name = name;
        this.type = type;
        this.homeNation = homeNation;
        this.supplyCenter = supplyCenter;
        this.coasts = coasts != null ? List.copyOf(coasts) : List.of();
        this.adjacencies = adjacencies != null ? Collections.unmodifiableMap(new HashMap<>(adjacencies)) : Map.of();
    }

    public String name() { return name; }
    public ProvinceType type() { return type; }
    public Optional<Nation> homeNation() { return Optional.ofNullable(homeNation); }
    public boolean isSupplyCenter() { return supplyCenter; }
    public List<Coast> coasts() { return coasts; }
    public Map<String, Coast> adjacencies() { return adjacencies; }

    public boolean isCoastal() { return type == ProvinceType.COASTAL; }
    public boolean isSea() { return type == ProvinceType.SEA; }
    public boolean isInland() { return type == ProvinceType.INLAND; }

    public boolean isAdjacentTo(String provinceName) {
        return adjacencies.containsKey(provinceName);
    }

    public Optional<Coast> adjacencyCoast(String provinceName) {
        return Optional.ofNullable(adjacencies.get(provinceName));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Province province)) return false;
        return Objects.equals(name, province.name);
    }

    @Override
    public int hashCode() { return Objects.hashCode(name); }
}
