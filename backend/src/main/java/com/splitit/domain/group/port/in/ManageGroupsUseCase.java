package com.splitit.domain.group.port.in;

import com.splitit.domain.group.model.GroupDetails;
import com.splitit.domain.group.model.GroupSummary;
import java.util.List;
import java.util.UUID;

public interface ManageGroupsUseCase {

    /** Creates a group; the creator is added as OWNER. Returns the full group view. */
    GroupDetails createGroup(UUID creatorId, CreateGroupCommand command);

    /** Groups the user belongs to. */
    List<GroupSummary> listGroups(UUID userId);

    /**
     * Full details of a group the user is a member of.
     *
     * @throws com.splitit.domain.group.exception.GroupNotFoundException if absent or not a member
     */
    GroupDetails getGroup(UUID userId, UUID groupId);

    record CreateGroupCommand(String name, String description) {
    }
}
