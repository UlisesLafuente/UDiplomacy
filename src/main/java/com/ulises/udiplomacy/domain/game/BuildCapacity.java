package com.ulises.udiplomacy.domain.game;

import java.util.Objects;

public final class BuildCapacity {
    private final Nation nation;
    private final int buildsAllowed;
    private final int disbandsRequired;

    public BuildCapacity(Nation nation, int buildsAllowed, int disbandsRequired) {
        this.nation = nation;
        this.buildsAllowed = buildsAllowed;
        this.disbandsRequired = disbandsRequired;
    }

    public Nation nation() { return nation; }
    public int buildsAllowed() { return buildsAllowed; }
    public int disbandsRequired() { return disbandsRequired; }
    public boolean canBuild() { return buildsAllowed > 0; }
    public boolean mustDisband() { return disbandsRequired > 0; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BuildCapacity that)) return false;
        return buildsAllowed == that.buildsAllowed
                && disbandsRequired == that.disbandsRequired
                && Objects.equals(nation, that.nation);
    }

    @Override
    public int hashCode() { return Objects.hash(nation, buildsAllowed, disbandsRequired); }
}
