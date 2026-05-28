package com.ulises.udiplomacy.domain.game;

import com.ulises.udiplomacy.domain.game.enums.UnitType;

import java.util.Objects;

public final class Unit {
    private final UnitType unitType;
    private final Nation nation;
    private final Territory location;

    public Unit(UnitType unitType, Nation nation, Territory location) {
        this.unitType = unitType;
        this.nation = nation;
        this.location = location;
    }

    public UnitType unitType() { return unitType; }
    public Nation nation() { return nation; }
    public Territory location() { return location; }

    public Unit relocatedTo(Territory newLocation) {
        return new Unit(unitType, nation, newLocation);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Unit unit)) return false;
        return unitType == unit.unitType
                && Objects.equals(nation, unit.nation)
                && Objects.equals(location, unit.location);
    }

    @Override
    public int hashCode() {
        return Objects.hash(unitType, nation, location);
    }

    @Override
    public String toString() {
        return unitType + " " + nation + " in " + location;
    }
}
