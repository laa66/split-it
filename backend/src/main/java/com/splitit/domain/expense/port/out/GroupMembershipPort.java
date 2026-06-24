package com.splitit.domain.expense.port.out;

import java.util.List;
import java.util.UUID;

/**
 * Narrow port used by the expense domain to query group membership.
 * Deliberately does not import domain/group classes — keeps bounded contexts isolated.
 * The infrastructure adapter delegates to the same SpringDataGroupMemberRepository.
 */
public interface GroupMembershipPort {

    boolean isMember(UUID groupId, UUID userId);

    /** Returns UUIDs of all members of the group. */
    List<UUID> listMemberIds(UUID groupId);

    /**
     * Returns the UUID of the group owner (OWNER role).
     * Returns null if the group has no owner (should not happen in valid data).
     */
    UUID findGroupOwnerId(UUID groupId);
}
