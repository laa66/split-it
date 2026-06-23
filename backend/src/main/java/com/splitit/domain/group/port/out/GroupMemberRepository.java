package com.splitit.domain.group.port.out;

import com.splitit.domain.group.model.GroupMember;
import com.splitit.domain.group.model.GroupSummary;
import com.splitit.domain.group.model.MemberRole;
import java.util.List;
import java.util.UUID;

public interface GroupMemberRepository {

    /** Adds a user to a group with the given role. Idempotency is enforced by the caller. */
    void addMember(UUID groupId, UUID userId, MemberRole role);

    boolean isMember(UUID groupId, UUID userId);

    /** Members of a group, enriched with directory data, ordered by role then display name. */
    List<GroupMember> findMembers(UUID groupId);

    /** Summaries of all groups the given user belongs to, including their role and member count. */
    List<GroupSummary> findGroupsForUser(UUID userId);
}
