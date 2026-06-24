package com.splitit.infrastructure.persistence.jpa;

import com.splitit.domain.expense.port.out.GroupMembershipPort;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Implements the narrow GroupMembershipPort used by the expense domain.
 * Delegates to the same SpringDataGroupMemberRepository used by the group domain adapter,
 * but through the expense-domain port — no cross-context model imports.
 */
@Component
public class GroupMembershipAdapter implements GroupMembershipPort {

    private final SpringDataGroupMemberRepository jpaRepository;

    public GroupMembershipAdapter(SpringDataGroupMemberRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public boolean isMember(UUID groupId, UUID userId) {
        return jpaRepository.existsByGroupIdAndUserId(groupId, userId);
    }

    @Override
    public List<UUID> listMemberIds(UUID groupId) {
        return jpaRepository.findMembersByGroupId(groupId).stream()
                .map(GroupMemberView::getUserId)
                .toList();
    }

    @Override
    public UUID findGroupOwnerId(UUID groupId) {
        return jpaRepository.findMembersByGroupId(groupId).stream()
                .filter(v -> "OWNER".equals(v.getRole()))
                .map(GroupMemberView::getUserId)
                .findFirst()
                .orElse(null);
    }
}
