package com.ulises.udiplomacy.domain.game;

import java.util.Objects;

public final class Coast {
    private final String name;

    public Coast(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Coast name must not be blank");
        }
        this.name = name;
    }

    public String name() { return name; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Coast coast)) return false;
        return Objects.equals(name, coast.name);
    }

    @Override
    public int hashCode() { return Objects.hashCode(name); }

    @Override
    public String toString() { return name; }
}
