package com.splitit.domain.user.model;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Pure domain model. No framework/persistence annotations.
 * passwordHash holds an already-encoded value — the domain never stores raw passwords.
 */
public final class User {

    private final UUID id;
    private final String email;
    private final String displayName;
    private final String passwordHash;
    private final OffsetDateTime createdAt;

    public User(UUID id, String email, String displayName, String passwordHash, OffsetDateTime createdAt) {
        this.id = id;
        this.email = email;
        this.displayName = displayName;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
    }

    /** Factory for a user not yet persisted: id and createdAt are assigned by the database. */
    public static User newUser(String email, String displayName, String passwordHash) {
        return new User(null, email, displayName, passwordHash, null);
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User other)) return false;
        return Objects.equals(id, other.id) && Objects.equals(email, other.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, email);
    }
}
