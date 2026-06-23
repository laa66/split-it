package com.splitit.domain.group.model;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Pure domain model for a group invitation. No framework/persistence annotations.
 * id, token and createdAt are assigned by the database when null.
 */
public final class Invitation {

    private final UUID id;
    private final UUID groupId;
    private final String invitedEmail;
    private final UUID invitedBy;
    private final UUID token;
    private final InvitationStatus status;
    private final OffsetDateTime expiresAt;
    private final OffsetDateTime createdAt;

    public Invitation(UUID id, UUID groupId, String invitedEmail, UUID invitedBy, UUID token,
                      InvitationStatus status, OffsetDateTime expiresAt, OffsetDateTime createdAt) {
        this.id = id;
        this.groupId = groupId;
        this.invitedEmail = invitedEmail;
        this.invitedBy = invitedBy;
        this.token = token;
        this.status = status;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    /** Factory for a new pending invitation: id, token and createdAt are assigned by the database. */
    public static Invitation newPending(UUID groupId, String invitedEmail, UUID invitedBy,
                                        OffsetDateTime expiresAt) {
        return new Invitation(null, groupId, invitedEmail, invitedBy, null,
                InvitationStatus.PENDING, expiresAt, null);
    }

    /** Returns a copy with the given status (immutable transition). */
    public Invitation withStatus(InvitationStatus newStatus) {
        return new Invitation(id, groupId, invitedEmail, invitedBy, token,
                newStatus, expiresAt, createdAt);
    }

    public boolean isExpired(OffsetDateTime now) {
        return expiresAt != null && expiresAt.isBefore(now);
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

    public InvitationStatus getStatus() {
        return status;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
