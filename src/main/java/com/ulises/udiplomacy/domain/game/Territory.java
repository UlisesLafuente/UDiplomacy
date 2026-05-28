package com.ulises.udiplomacy.domain.game;

import java.util.Objects;
import java.util.Optional;

public final class Territory {
    private final String provinceName;
    private final Coast coast;

    public Territory(String provinceName) {
        this(provinceName, null);
    }

    public Territory(String provinceName, Coast coast) {
        if (provinceName == null || provinceName.isBlank()) {
            throw new IllegalArgumentException("Province name must not be blank");
        }
        this.provinceName = provinceName;
        this.coast = coast;
    }

    public String provinceName() { return provinceName; }
    public Optional<Coast> coast() { return Optional.ofNullable(coast); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Territory territory)) return false;
        return Objects.equals(provinceName, territory.provinceName)
                && Objects.equals(coast, territory.coast);
    }

    @Override
    public int hashCode() { return Objects.hash(provinceName, coast); }

    @Override
    public String toString() {
        return coast != null ? provinceName + "/" + coast : provinceName;
    }
}
