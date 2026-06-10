package com.ulises.udiplomacy.domain.game;

import java.util.Objects;

public final class BuildCapacity {
    private final Nation nation;
    private final int buildsAllowed;
    private final int disbandsRequired;
    private final int colonialBuildsAvailable;

    public BuildCapacity(Nation nation, int buildsAllowed, int disbandsRequired) {
        this(nation, buildsAllowed, disbandsRequired, 0);
    }

    public BuildCapacity(Nation nation, int buildsAllowed, int disbandsRequired, int colonialBuildsAvailable) {
        this.nation = nation;
        this.buildsAllowed = buildsAllowed;
        this.disbandsRequired = disbandsRequired;
        this.colonialBuildsAvailable = colonialBuildsAvailable;
    }

    public Nation nation() { return nation; }
    public int buildsAllowed() { return buildsAllowed; }
    public int disbandsRequired() { return disbandsRequired; }
    public int colonialBuildsAvailable() { return colonialBuildsAvailable; }
    public boolean canBuild() { return buildsAllowed > 0 || colonialBuildsAvailable > 0; }
    public boolean mustDisband() { return disbandsRequired > 0; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BuildCapacity that)) return false;
        return buildsAllowed == that.buildsAllowed
                && disbandsRequired == that.disbandsRequired
                && colonialBuildsAvailable == that.colonialBuildsAvailable
                && Objects.equals(nation, that.nation);
    }

    @Override
    public int hashCode() { return Objects.hash(nation, buildsAllowed, disbandsRequired, colonialBuildsAvailable); }
}
