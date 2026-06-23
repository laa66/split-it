package com.splitit.domain.group.port.out;

import com.splitit.domain.group.model.Group;
import java.util.Optional;
import java.util.UUID;

public interface GroupRepository {

    /** Persists a new group and returns it with database-assigned id and createdAt. */
    Group save(Group group);

    Optional<Group> findById(UUID groupId);
}
