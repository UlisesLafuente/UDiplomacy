package com.ulises.udiplomacy.domain.game;

import java.util.*;

public final class GameMap {
    private final String id;
    private final String name;
    private final Map<String, Province> provinces;

    public GameMap(String id, String name, List<Province> provinces) {
        this.id = id;
        this.name = name;
        this.provinces = new HashMap<>();
        for (Province p : provinces) {
            this.provinces.put(p.name(), p);
        }
    }

    public String id() { return id; }
    public String name() { return name; }
    public Map<String, Province> provinces() { return Collections.unmodifiableMap(provinces); }

    public Optional<Province> province(String name) {
        return Optional.ofNullable(provinces.get(name));
    }

    public boolean hasProvince(String name) {
        return provinces.containsKey(name);
    }

    public List<Province> supplyCenters() {
        return provinces.values().stream()
                .filter(Province::isSupplyCenter)
                .toList();
    }

    public List<String> supplyCenterNames() {
        return supplyCenters().stream()
                .map(Province::name)
                .toList();
    }

    public List<Province> homeCentersFor(Nation nation) {
        return provinces.values().stream()
                .filter(p -> p.homeNation().map(nation::equals).orElse(false))
                .filter(Province::isSupplyCenter)
                .toList();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GameMap gameMap)) return false;
        return Objects.equals(id, gameMap.id);
    }

    @Override
    public int hashCode() { return Objects.hashCode(id); }
}
