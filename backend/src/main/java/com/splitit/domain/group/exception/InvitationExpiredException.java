package com.splitit.domain.group.exception;

/** Thrown when accepting an invitation whose expiry has passed. Maps to 410 Gone. */
public class InvitationExpiredException extends RuntimeException {

    public InvitationExpiredException() {
        super("Invitation has expired");
    }
}
