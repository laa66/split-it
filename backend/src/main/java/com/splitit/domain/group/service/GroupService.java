package com.splitit.domain.group.service;

import com.splitit.domain.group.exception.GroupNotFoundException;
import com.splitit.domain.group.model.Group;
import com.splitit.domain.group.model.GroupDetails;
import com.splitit.domain.group.model.GroupSummary;
import com.splitit.domain.group.model.MemberRole;
import com.splitit.domain.group.port.in.ManageGroupsUseCase;
import com.splitit.domain.group.port.out.GroupMemberRepository;
import com.splitit.domain.group.port.out.GroupRepository;
import java.util.List;
import java.util.UUID;

/**
 * Pure domain service. Depends only on out-ports, never on Spring or persistence types.
 * Authorization is enforced here, not just in the controller: a user may only see groups
 * they belong to; non-members get GroupNotFoundException (404) to avoid leaking existence.
 */
public class GroupService implements ManageGroupsUseCase {

    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_DESCRIPTION_LENGTH = 500;

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;

    public GroupService(GroupRepository groupRepository, GroupMemberRepository groupMemberRepository) {
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
    }

    @Override
    public GroupDetails createGroup(UUID creatorId, CreateGroupCommand command) {
        String name = command.name() == null ? null : command.name().trim();
        String description = normalizeDescription(command.description());
        validate(name, description);

        Group saved = groupRepository.save(Group.newGroup(name, description, creatorId));
        groupMemberRepository.addMember(saved.getId(), creatorId, MemberRole.OWNER);

        return new GroupDetails(saved, groupMemberRepository.findMembers(saved.getId()));
    }

    @Override
    public List<GroupSummary> listGroups(UUID userId) {
        return groupMemberRepository.findGroupsForUser(userId);
    }

    @Override
    public GroupDetails getGroup(UUID userId, UUID groupId) {
        if (!groupMemberRepository.isMember(groupId, userId)) {
            throw new GroupNotFoundException(groupId);
        }
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new GroupNotFoundException(groupId));
        return new GroupDetails(group, groupMemberRepository.findMembers(groupId));
    }

    private void validate(String name, String description) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Group name must not be empty");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("Group name must be at most " + MAX_NAME_LENGTH + " characters");
        }
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException(
                    "Group description must be at most " + MAX_DESCRIPTION_LENGTH + " characters");
        }
    }

    private String normalizeDescription(String description) {
        if (description == null) {
            return null;
        }
        String trimmed = description.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
