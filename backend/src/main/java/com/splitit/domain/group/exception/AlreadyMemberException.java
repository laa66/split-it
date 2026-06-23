package com.splitit.domain.group.exception;

/** Thrown when inviting an email that already belongs to a member of the group. Maps to 409. */
public class AlreadyMemberException extends RuntimeException {

    public AlreadyMemberException(String email) {
        super("User is already a member of this group: " + email);
    }
}
