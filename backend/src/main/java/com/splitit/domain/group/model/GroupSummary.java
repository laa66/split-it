package com.splitit.domain.group.model;

import java.time.OffsetDateTime;
import java.util.UUID;

/** A group as seen in the current user's group list, including their own role and member count. */
public record GroupSummary(UUID id, String name, String description, MemberRole currentUserRole,
                           int membersCount, OffsetDateTime createdAt) {
}
