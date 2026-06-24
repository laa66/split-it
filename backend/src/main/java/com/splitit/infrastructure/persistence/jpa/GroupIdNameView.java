package com.splitit.infrastructure.persistence.jpa;

import java.util.UUID;

/** Minimal projection of a group: id and name. Used by reminder and report adapters. */
public interface GroupIdNameView {
    UUID getId();
    String getName();
}
