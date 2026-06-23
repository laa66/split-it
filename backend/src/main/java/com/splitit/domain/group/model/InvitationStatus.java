package com.splitit.domain.group.model;

/** Lifecycle of an invitation. Maps 1:1 to the invitations.status CHECK constraint. */
public enum InvitationStatus {
    PENDING,
    ACCEPTED,
    EXPIRED
}
