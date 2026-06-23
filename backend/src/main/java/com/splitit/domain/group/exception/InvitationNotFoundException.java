package com.splitit.domain.group.exception;

/** Thrown when no invitation matches the given token. Maps to 404. */
public class InvitationNotFoundException extends RuntimeException {

    public InvitationNotFoundException() {
        super("Invitation not found");
    }
}
