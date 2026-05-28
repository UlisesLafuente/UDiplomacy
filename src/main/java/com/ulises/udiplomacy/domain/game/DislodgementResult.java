package com.ulises.udiplomacy.domain.game;

import java.util.*;

public final class DislodgementResult {
    private final Map<Unit, List<Territory>> dislodgedUnits;
    private final Set<String> contestedProvinces;

    public DislodgementResult(Map<Unit, List<Territory>> dislodgedUnits, Set<String> contestedProvinces) {
        this.dislodgedUnits = new HashMap<>(dislodgedUnits);
        this.contestedProvinces = new HashSet<>(contestedProvinces);
    }

    public Map<Unit, List<Territory>> dislodgedUnits() {
        return Collections.unmodifiableMap(dislodgedUnits);
    }

    public Set<String> contestedProvinces() {
        return Collections.unmodifiableSet(contestedProvinces);
    }

    public boolean hasDislodgedUnits() { return !dislodgedUnits.isEmpty(); }

    public boolean isProvinceContested(String provinceName) {
        return contestedProvinces.contains(provinceName);
    }

    public List<Territory> retreatOptionsFor(Unit unit) {
        return dislodgedUnits.getOrDefault(unit, List.of());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DislodgementResult that)) return false;
        return Objects.equals(dislodgedUnits, that.dislodgedUnits)
                && Objects.equals(contestedProvinces, that.contestedProvinces);
    }

    @Override
    public int hashCode() { return Objects.hash(dislodgedUnits, contestedProvinces); }
}
