package com.splitit.domain.group.port.out;

import com.splitit.domain.group.model.Invitation;
import com.splitit.domain.group.model.InvitationStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvitationRepository {

    /** Persists a new invitation and returns it with database-assigned id, token and createdAt. */
    Invitation save(Invitation invitation);

    /** Updates the status of an existing invitation. */
    Invitation update(Invitation invitation);

    Optional<Invitation> findByToken(UUID token);

    /** Pending invitations addressed to the given (normalized) email across all groups. */
    List<Invitation> findByEmailAndStatus(String invitedEmail, InvitationStatus status);

    /**
     * The PENDING invitation for the given group and (normalized) email, if any.
     * Expiry is evaluated by the caller via {@link Invitation#isExpired}, so re-invites
     * stay idempotent while expired pending rows are treated as absent.
     */
    Optional<Invitation> findActivePending(UUID groupId, String invitedEmail);
}
