package com.splitit.domain.group.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.splitit.domain.group.exception.GroupNotFoundException;
import com.splitit.domain.group.model.Group;
import com.splitit.domain.group.model.GroupDetails;
import com.splitit.domain.group.model.GroupMember;
import com.splitit.domain.group.model.MemberRole;
import com.splitit.domain.group.port.in.ManageGroupsUseCase.CreateGroupCommand;
import com.splitit.domain.group.port.out.GroupMemberRepository;
import com.splitit.domain.group.port.out.GroupRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class GroupServiceTest {

    private GroupRepository groupRepository;
    private GroupMemberRepository groupMemberRepository;
    private GroupService groupService;

    private final UUID creatorId = UUID.randomUUID();
    private final UUID groupId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        groupRepository = mock(GroupRepository.class);
        groupMemberRepository = mock(GroupMemberRepository.class);
        groupService = new GroupService(groupRepository, groupMemberRepository);
    }

    private Group persistedGroup(String name, String description) {
        return new Group(groupId, name, description, creatorId, OffsetDateTime.now());
    }

    @Test
    void createGroup_addsCreatorAsOwner_andReturnsDetails() {
        when(groupRepository.save(any())).thenReturn(persistedGroup("Trip", "Summer"));
        when(groupMemberRepository.findMembers(groupId)).thenReturn(
                List.of(new GroupMember(creatorId, "Alice", "alice@example.com", MemberRole.OWNER)));

        GroupDetails details = groupService.createGroup(creatorId,
                new CreateGroupCommand("Trip", "Summer"));

        verify(groupMemberRepository).addMember(groupId, creatorId, MemberRole.OWNER);
        assertThat(details.group().getName()).isEqualTo("Trip");
        assertThat(details.members()).hasSize(1);
        assertThat(details.members().get(0).role()).isEqualTo(MemberRole.OWNER);
    }

    @Test
    void createGroup_trimsName_andNullifiesBlankDescription() {
        when(groupRepository.save(any())).thenReturn(persistedGroup("Trip", null));
        when(groupMemberRepository.findMembers(any())).thenReturn(List.of());

        groupService.createGroup(creatorId, new CreateGroupCommand("  Trip  ", "   "));

        ArgumentCaptor<Group> captor = ArgumentCaptor.forClass(Group.class);
        verify(groupRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Trip");
        assertThat(captor.getValue().getDescription()).isNull();
    }

    @Test
    void createGroup_blankName_throws_andNothingSaved() {
        assertThatThrownBy(() ->
                groupService.createGroup(creatorId, new CreateGroupCommand("   ", "x")))
                .isInstanceOf(IllegalArgumentException.class);
        verify(groupRepository, never()).save(any());
    }

    @Test
    void listGroups_delegatesToRepository() {
        groupService.listGroups(creatorId);
        verify(groupMemberRepository).findGroupsForUser(creatorId);
    }

    @Test
    void getGroup_member_returnsDetails() {
        when(groupMemberRepository.isMember(groupId, creatorId)).thenReturn(true);
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(persistedGroup("Trip", "d")));
        when(groupMemberRepository.findMembers(groupId)).thenReturn(List.of());

        GroupDetails details = groupService.getGroup(creatorId, groupId);

        assertThat(details.group().getId()).isEqualTo(groupId);
    }

    @Test
    void getGroup_nonMember_throwsNotFound_andDoesNotLoadGroup() {
        when(groupMemberRepository.isMember(eq(groupId), any())).thenReturn(false);

        assertThatThrownBy(() -> groupService.getGroup(UUID.randomUUID(), groupId))
                .isInstanceOf(GroupNotFoundException.class);
        verify(groupRepository, never()).findById(any());
    }
}
