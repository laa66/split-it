package com.splitit.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Maps exactly to the `invitations` table in db/init.sql. ddl-auto=validate.
 * token and status carry DB defaults; the adapter assigns them when missing.
 */
@Entity
@Table(name = "invitations")
public class InvitationJpaEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "group_id", nullable = false, updatable = false)
    private UUID groupId;

    @Column(name = "invited_email", nullable = false, length = 255)
    private String invitedEmail;

    @Column(name = "invited_by", nullable = false, updatable = false)
    private UUID invitedBy;

    @Column(name = "token", nullable = false, unique = true, updatable = false)
    private UUID token;

    @Column(name = "status", nullable = false, length = 10)
    private String status;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected InvitationJpaEntity() {
    }

    public InvitationJpaEntity(UUID id, UUID groupId, String invitedEmail, UUID invitedBy, UUID token,
                               String status, OffsetDateTime expiresAt, OffsetDateTime createdAt) {
        this.id = id;
        this.groupId = groupId;
        this.invitedEmail = invitedEmail;
        this.invitedBy = invitedBy;
        this.token = token;
        this.status = status;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public UUID getId() {
        return id;
    }

    public UUID getGroupId() {
        return groupId;
    }

    public String getInvitedEmail() {
        return invitedEmail;
    }

    public UUID getInvitedBy() {
        return invitedBy;
    }

    public UUID getToken() {
        return token;
    }

    public String getStatus() {
        return status;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
