package com.splitit.domain.group.model;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Pure domain model for a group. No framework/persistence annotations.
 */
public final class Group {

    private final UUID id;
    private final String name;
    private final String description;
    private final UUID createdBy;
    private final OffsetDateTime createdAt;

    public Group(UUID id, String name, String description, UUID createdBy, OffsetDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    /** Factory for a group not yet persisted: id and createdAt are assigned by the database. */
    public static Group newGroup(String name, String description, UUID createdBy) {
        return new Group(null, name, description, createdBy, null);
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Group group)) {
            return false;
        }
        return Objects.equals(id, group.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
