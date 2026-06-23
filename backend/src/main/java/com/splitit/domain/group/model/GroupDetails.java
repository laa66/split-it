package com.splitit.domain.group.model;

import java.util.List;

/** Full view of a group: the group itself plus its member list. */
public record GroupDetails(Group group, List<GroupMember> members) {
}
