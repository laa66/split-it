package com.splitit.infrastructure.persistence.jpa;

import com.splitit.domain.group.model.GroupMember;
import com.splitit.domain.group.model.GroupSummary;
import com.splitit.domain.group.model.MemberRole;
import com.splitit.domain.group.port.out.GroupMemberRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class GroupMemberRepositoryAdapter implements GroupMemberRepository {

    private final SpringDataGroupMemberRepository jpaRepository;

    public GroupMemberRepositoryAdapter(SpringDataGroupMemberRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void addMember(UUID groupId, UUID userId, MemberRole role) {
        GroupMemberJpaEntity entity = new GroupMemberJpaEntity(
                UUID.randomUUID(), groupId, userId, role.name(), OffsetDateTime.now());
        jpaRepository.save(entity);
    }

    @Override
    public boolean isMember(UUID groupId, UUID userId) {
        return jpaRepository.existsByGroupIdAndUserId(groupId, userId);
    }

    @Override
    public List<GroupMember> findMembers(UUID groupId) {
        return jpaRepository.findMembersByGroupId(groupId).stream()
                .map(v -> new GroupMember(v.getUserId(), v.getDisplayName(), v.getEmail(),
                        MemberRole.valueOf(v.getRole())))
                .toList();
    }

    @Override
    public List<GroupSummary> findGroupsForUser(UUID userId) {
        return jpaRepository.findGroupsForUser(userId).stream()
                .map(v -> new GroupSummary(v.getId(), v.getName(), v.getDescription(),
                        MemberRole.valueOf(v.getCurrentUserRole()),
                        (int) v.getMembersCount(), v.getCreatedAt().atOffset(ZoneOffset.UTC)))
                .toList();
    }
}
