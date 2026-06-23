package com.splitit.domain.group.port.in;

import java.util.UUID;

public interface AcceptInvitationUseCase {

    /**
     * Accepts an invitation by its token.
     * <p>
     * If the invited email already has an account, the user is added to the group and the
     * invitation is marked ACCEPTED. If no account exists yet, the result flags that
     * registration is required (the invitation stays PENDING).
     *
     * @throws com.splitit.domain.group.exception.InvitationNotFoundException unknown token
     * @throws com.splitit.domain.group.exception.InvitationNotPendingException already accepted/expired
     * @throws com.splitit.domain.group.exception.InvitationExpiredException past expiry (status set to EXPIRED)
     */
    AcceptResult accept(UUID token);

    record AcceptResult(UUID groupId, String groupName, String invitedEmail,
                        boolean requiresRegistration) {
    }
}
