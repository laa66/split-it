package com.splitit.domain.group.exception;

import java.util.UUID;

/**
 * Thrown when a group does not exist OR the requester is not a member.
 * Non-members are deliberately given a 404 (not 403) so the API never reveals the
 * existence of groups the user has no access to.
 */
public class GroupNotFoundException extends RuntimeException {

    public GroupNotFoundException(UUID groupId) {
        super("Group not found: " + groupId);
    }
}
