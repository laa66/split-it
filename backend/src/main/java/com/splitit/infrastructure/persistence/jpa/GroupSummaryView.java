package com.splitit.infrastructure.persistence.jpa;

import java.time.Instant;
import java.util.UUID;

/** Projection of a group as it appears in a user's group list, with their role and member count. */
public interface GroupSummaryView {

    UUID getId();

    String getName();

    String getDescription();

    String getCurrentUserRole();

    long getMembersCount();

    Instant getCreatedAt();
}
