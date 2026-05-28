package com.ulises.udiplomacy.domain.shared;

import java.time.Instant;
import java.util.Objects;

public abstract class BaseEntity {
    private final String id;
    private final Instant createdAt;
    private Instant updatedAt;

    protected BaseEntity(String id) {
        this.id = id;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public String id() { return id; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }

    protected void touch() {
        this.updatedAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BaseEntity that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hashCode(id); }
}
