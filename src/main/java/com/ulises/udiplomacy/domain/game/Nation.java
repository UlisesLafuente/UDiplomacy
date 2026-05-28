package com.ulises.udiplomacy.domain.game;

import java.util.Objects;

public final class Nation {
    private final String name;

    public Nation(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Nation name must not be blank");
        }
        this.name = name;
    }

    public String name() { return name; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Nation nation)) return false;
        return Objects.equals(name, nation.name);
    }

    @Override
    public int hashCode() { return Objects.hashCode(name); }

    @Override
    public String toString() { return name; }
}
