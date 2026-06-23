package com.splitit.infrastructure.persistence.jpa;

import java.util.UUID;

/** Projection of a group member joined with directory data for the member list. */
public interface GroupMemberView {

    UUID getUserId();

    String getDisplayName();

    String getEmail();

    String getRole();
}
