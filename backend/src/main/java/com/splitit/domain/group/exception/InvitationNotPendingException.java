package com.splitit.domain.group.exception;

import com.splitit.domain.group.model.InvitationStatus;

/** Thrown when accepting an invitation that is not PENDING (already accepted or expired). Maps to 409. */
public class InvitationNotPendingException extends RuntimeException {

    public InvitationNotPendingException(InvitationStatus status) {
        super("Invitation is no longer pending (status: " + status + ")");
    }
}
