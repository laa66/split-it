package com.splitit.infrastructure.security;

import java.util.UUID;

/** Principal placed in the SecurityContext after a valid JWT is parsed. */
public record AuthenticatedUser(UUID id, String email) {
}
