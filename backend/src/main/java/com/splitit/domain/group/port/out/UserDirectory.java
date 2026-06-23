package com.splitit.domain.group.port.out;

import com.splitit.domain.group.model.DirectoryUser;
import java.util.Optional;

/**
 * Read-only access to user identities the group domain needs, without coupling to the
 * user domain model. Backed by the same users table via an adapter.
 */
public interface UserDirectory {

    Optional<DirectoryUser> findByEmail(String email);
}
