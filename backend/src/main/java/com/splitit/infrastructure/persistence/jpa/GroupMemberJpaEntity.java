package com.splitit.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Maps exactly to the `group_members` table in db/init.sql. ddl-auto=validate.
 * role stored as its String name (OWNER/MEMBER) to satisfy the CHECK constraint.
 */
@Entity
@Table(name = "group_members")
public class GroupMemberJpaEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "group_id", nullable = false, updatable = false)
    private UUID groupId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "role", nullable = false, length = 10)
    private String role;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private OffsetDateTime joinedAt;

    protected GroupMemberJpaEntity() {
    }

    public GroupMemberJpaEntity(UUID id, UUID groupId, UUID userId, String role,
                                OffsetDateTime joinedAt) {
        this.id = id;
        this.groupId = groupId;
        this.userId = userId;
        this.role = role;
        this.joinedAt = joinedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getGroupId() {
        return groupId;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getRole() {
        return role;
    }

    public OffsetDateTime getJoinedAt() {
        return joinedAt;
    }
}
