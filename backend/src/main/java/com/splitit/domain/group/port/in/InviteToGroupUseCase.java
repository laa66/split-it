package com.splitit.domain.group.port.in;

import java.util.UUID;

public interface InviteToGroupUseCase {

    /**
     * Invites an email to a group. Only a member of the group may invite.
     * <p>
     * If the email already has an account, the user is added immediately (MEMBER) and an
     * "added to group" email is sent. Otherwise a PENDING invitation is created and a
     * registration-link email is sent.
     *
     * @throws com.splitit.domain.group.exception.GroupNotFoundException if absent or inviter not a member
     * @throws com.splitit.domain.group.exception.AlreadyMemberException if the email is already a member
     */
    InviteResult invite(UUID inviterId, UUID groupId, String email);

    /** Outcome of an invitation, so the API can communicate which path was taken. */
    record InviteResult(boolean addedImmediately) {
    }
}
