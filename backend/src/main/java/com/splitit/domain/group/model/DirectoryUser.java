package com.splitit.domain.group.model;

import java.util.UUID;

/**
 * Lightweight projection of a user as the group domain needs it, decoupling this module
 * from the user domain model. Populated by the UserDirectory out-port adapter.
 */
public record DirectoryUser(UUID id, String email, String displayName) {
}
