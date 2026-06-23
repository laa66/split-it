package com.splitit.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Maps exactly to the `groups` table in db/init.sql. ddl-auto=validate, so columns must align.
 * id and created_at carry DB defaults; the adapter assigns them when missing.
 */
@Entity
@Table(name = "groups")
public class GroupJpaEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected GroupJpaEntity() {
    }

    public GroupJpaEntity(UUID id, String name, String description, UUID createdBy,
                          OffsetDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
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
}
