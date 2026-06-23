package com.splitit.domain.group.port.in;

import java.util.UUID;

public interface ClaimPendingInvitationsUseCase {

    /**
     * Called right after a user registers: finds all PENDING, non-expired invitations for the
     * user's email, adds them to those groups, and marks the invitations ACCEPTED.
     * Expired invitations encountered along the way are marked EXPIRED.
     */
    void claimFor(UUID userId, String email);
}
