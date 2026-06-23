package com.splitit.domain.group.model;

import java.util.UUID;

/**
 * A user's membership in a group, enriched with directory data (display name, email)
 * so the API can render a member list without a second lookup.
 */
public record GroupMember(UUID userId, String displayName, String email, MemberRole role) {
}
