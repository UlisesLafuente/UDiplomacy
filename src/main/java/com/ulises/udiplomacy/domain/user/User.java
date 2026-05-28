package com.ulises.udiplomacy.domain.user;

import java.util.*;

public final class User {
    private final String userId;
    private final String username;
    private final String passwordHash;
    private final Role role;
    private final List<GameReference> gameReferences;

    public User(String userId, String username, String passwordHash, Role role) {
        this(userId, username, passwordHash, role, List.of());
    }

    public User(String userId, String username, String passwordHash, Role role,
                List<GameReference> gameReferences) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.gameReferences = List.copyOf(gameReferences);
    }

    public String userId() { return userId; }
    public String username() { return username; }
    public String passwordHash() { return passwordHash; }
    public Role role() { return role; }
    public List<GameReference> gameReferences() { return gameReferences; }
    public boolean isAdmin() { return role == Role.ADMIN; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User user)) return false;
        return Objects.equals(userId, user.userId);
    }

    @Override
    public int hashCode() { return Objects.hashCode(userId); }
}
