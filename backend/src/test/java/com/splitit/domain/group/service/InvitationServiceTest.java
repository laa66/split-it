package com.splitit.domain.group.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.splitit.domain.group.exception.AlreadyMemberException;
import com.splitit.domain.group.exception.GroupNotFoundException;
import com.splitit.domain.group.exception.InvitationExpiredException;
import com.splitit.domain.group.exception.InvitationNotFoundException;
import com.splitit.domain.group.exception.InvitationNotPendingException;
import com.splitit.domain.group.model.DirectoryUser;
import com.splitit.domain.group.model.Group;
import com.splitit.domain.group.model.Invitation;
import com.splitit.domain.group.model.InvitationStatus;
import com.splitit.domain.group.model.MemberRole;
import com.splitit.domain.group.port.in.AcceptInvitationUseCase.AcceptResult;
import com.splitit.domain.group.port.in.InviteToGroupUseCase.InviteResult;
import com.splitit.domain.group.port.out.EmailSender;
import com.splitit.domain.group.port.out.GroupMemberRepository;
import com.splitit.domain.group.port.out.GroupRepository;
import com.splitit.domain.group.port.out.InvitationRepository;
import com.splitit.domain.group.port.out.UserDirectory;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class InvitationServiceTest {

    private GroupRepository groupRepository;
    private GroupMemberRepository groupMemberRepository;
    private InvitationRepository invitationRepository;
    private UserDirectory userDirectory;
    private EmailSender emailSender;
    private InvitationService service;

    private final Clock clock = Clock.fixed(Instant.parse("2026-06-23T12:00:00Z"), ZoneOffset.UTC);
    private final OffsetDateTime now = OffsetDateTime.now(clock);

    private final UUID inviterId = UUID.randomUUID();
    private final UUID groupId = UUID.randomUUID();
    private final UUID token = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        groupRepository = mock(GroupRepository.class);
        groupMemberRepository = mock(GroupMemberRepository.class);
        invitationRepository = mock(InvitationRepository.class);
        userDirectory = mock(UserDirectory.class);
        emailSender = mock(EmailSender.class);
        service = new InvitationService(groupRepository, groupMemberRepository, invitationRepository,
                userDirectory, emailSender, Duration.ofDays(7), "http://localhost:5173", clock);
    }

    private Group group() {
        return new Group(groupId, "Trip", "Summer", inviterId, now);
    }

    private Invitation persistedPending(String email, OffsetDateTime expiresAt) {
        return new Invitation(UUID.randomUUID(), groupId, email, inviterId, token,
                InvitationStatus.PENDING, expiresAt, now);
    }

    // --- invite ---

    @Test
    void invite_nonMemberInviter_throwsNotFound() {
        when(groupMemberRepository.isMember(groupId, inviterId)).thenReturn(false);

        assertThatThrownBy(() -> service.invite(inviterId, groupId, "x@example.com"))
                .isInstanceOf(GroupNotFoundException.class);
        verify(invitationRepository, never()).save(any());
    }

    @Test
    void invite_existingUser_addsMemberImmediately_andSendsAddedEmail() {
        UUID invitedUserId = UUID.randomUUID();
        when(groupMemberRepository.isMember(groupId, inviterId)).thenReturn(true);
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group()));
        when(userDirectory.findByEmail("bob@example.com"))
                .thenReturn(Optional.of(new DirectoryUser(invitedUserId, "bob@example.com", "Bob")));
        when(groupMemberRepository.isMember(groupId, invitedUserId)).thenReturn(false);
        when(invitationRepository.save(any()))
                .thenReturn(persistedPending("bob@example.com", now.plusDays(7)));

        InviteResult result = service.invite(inviterId, groupId, "BOB@example.com");

        assertThat(result.addedImmediately()).isTrue();
        verify(groupMemberRepository).addMember(groupId, invitedUserId, MemberRole.MEMBER);
        verify(emailSender).sendAddedToGroup("bob@example.com", "Trip");
        verify(emailSender, never()).sendGroupInvitation(anyString(), anyString(), anyString());
        // audit invitation transitioned to ACCEPTED
        ArgumentCaptor<Invitation> captor = ArgumentCaptor.forClass(Invitation.class);
        verify(invitationRepository).update(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(InvitationStatus.ACCEPTED);
    }

    @Test
    void invite_existingUserAlreadyMember_throwsConflict() {
        UUID invitedUserId = UUID.randomUUID();
        when(groupMemberRepository.isMember(groupId, inviterId)).thenReturn(true);
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group()));
        when(userDirectory.findByEmail("bob@example.com"))
                .thenReturn(Optional.of(new DirectoryUser(invitedUserId, "bob@example.com", "Bob")));
        when(groupMemberRepository.isMember(groupId, invitedUserId)).thenReturn(true);

        assertThatThrownBy(() -> service.invite(inviterId, groupId, "bob@example.com"))
                .isInstanceOf(AlreadyMemberException.class);
        verify(groupMemberRepository, never()).addMember(any(), eq(invitedUserId), any());
    }

    @Test
    void invite_unknownEmail_createsPending_andSendsInvitationWithRegistrationLink() {
        when(groupMemberRepository.isMember(groupId, inviterId)).thenReturn(true);
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group()));
        when(userDirectory.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(invitationRepository.save(any()))
                .thenReturn(persistedPending("new@example.com", now.plusDays(7)));

        InviteResult result = service.invite(inviterId, groupId, "new@example.com");

        assertThat(result.addedImmediately()).isFalse();
        verify(groupMemberRepository, never()).addMember(any(), any(), any());
        verify(emailSender).sendGroupInvitation(eq("new@example.com"), eq("Trip"),
                eq("http://localhost:5173/register?invitation=" + token));
    }

    @Test
    void invite_unknownEmailTwice_isIdempotent_singlePendingSingleEmail() {
        when(groupMemberRepository.isMember(groupId, inviterId)).thenReturn(true);
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group()));
        when(userDirectory.findByEmail("new@example.com")).thenReturn(Optional.empty());

        Invitation persisted = persistedPending("new@example.com", now.plusDays(7));
        when(invitationRepository.save(any())).thenReturn(persisted);
        // First call: no active pending yet; second call: the one just created is active.
        when(invitationRepository.findActivePending(groupId, "new@example.com"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(persisted));

        InviteResult first = service.invite(inviterId, groupId, "new@example.com");
        InviteResult second = service.invite(inviterId, groupId, "new@example.com");

        assertThat(first.addedImmediately()).isFalse();
        assertThat(second.addedImmediately()).isFalse();
        // Exactly one PENDING row persisted and one invitation email sent across both calls.
        verify(invitationRepository, org.mockito.Mockito.times(1)).save(any());
        verify(emailSender, org.mockito.Mockito.times(1))
                .sendGroupInvitation(eq("new@example.com"), eq("Trip"), anyString());
        // The still-active pending row is left untouched (no status churn).
        verify(invitationRepository, never()).update(any());
    }

    @Test
    void invite_unknownEmail_existingPendingExpired_issuesFreshInvitation() {
        when(groupMemberRepository.isMember(groupId, inviterId)).thenReturn(true);
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group()));
        when(userDirectory.findByEmail("new@example.com")).thenReturn(Optional.empty());
        // An expired PENDING row is returned; the service marks it EXPIRED and issues a fresh one.
        Invitation expiredPending = persistedPending("new@example.com", now.minusDays(1));
        when(invitationRepository.findActivePending(groupId, "new@example.com"))
                .thenReturn(Optional.of(expiredPending));
        when(invitationRepository.save(any()))
                .thenReturn(persistedPending("new@example.com", now.plusDays(7)));

        InviteResult result = service.invite(inviterId, groupId, "new@example.com");

        assertThat(result.addedImmediately()).isFalse();
        ArgumentCaptor<Invitation> captor = ArgumentCaptor.forClass(Invitation.class);
        verify(invitationRepository).update(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(InvitationStatus.EXPIRED);
        verify(invitationRepository).save(any());
        verify(emailSender).sendGroupInvitation(anyString(), anyString(), anyString());
    }

    // --- accept ---

    @Test
    void accept_unknownToken_throwsNotFound() {
        when(invitationRepository.findByToken(token)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.accept(token))
                .isInstanceOf(InvitationNotFoundException.class);
    }

    @Test
    void accept_alreadyAccepted_throwsNotPending() {
        Invitation accepted = persistedPending("bob@example.com", now.plusDays(7))
                .withStatus(InvitationStatus.ACCEPTED);
        when(invitationRepository.findByToken(token)).thenReturn(Optional.of(accepted));

        assertThatThrownBy(() -> service.accept(token))
                .isInstanceOf(InvitationNotPendingException.class);
    }

    @Test
    void accept_expired_marksExpired_andThrows() {
        Invitation expired = persistedPending("bob@example.com", now.minusDays(1));
        when(invitationRepository.findByToken(token)).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.accept(token))
                .isInstanceOf(InvitationExpiredException.class);

        ArgumentCaptor<Invitation> captor = ArgumentCaptor.forClass(Invitation.class);
        verify(invitationRepository).update(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(InvitationStatus.EXPIRED);
    }

    @Test
    void accept_existingUser_addsMember_andMarksAccepted() {
        UUID invitedUserId = UUID.randomUUID();
        Invitation pending = persistedPending("bob@example.com", now.plusDays(7));
        when(invitationRepository.findByToken(token)).thenReturn(Optional.of(pending));
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group()));
        when(userDirectory.findByEmail("bob@example.com"))
                .thenReturn(Optional.of(new DirectoryUser(invitedUserId, "bob@example.com", "Bob")));
        when(groupMemberRepository.isMember(groupId, invitedUserId)).thenReturn(false);

        AcceptResult result = service.accept(token);

        assertThat(result.requiresRegistration()).isFalse();
        assertThat(result.groupId()).isEqualTo(groupId);
        verify(groupMemberRepository).addMember(groupId, invitedUserId, MemberRole.MEMBER);
        ArgumentCaptor<Invitation> captor = ArgumentCaptor.forClass(Invitation.class);
        verify(invitationRepository).update(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(InvitationStatus.ACCEPTED);
    }

    @Test
    void accept_noAccountYet_requiresRegistration_andStaysPending() {
        Invitation pending = persistedPending("ghost@example.com", now.plusDays(7));
        when(invitationRepository.findByToken(token)).thenReturn(Optional.of(pending));
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group()));
        when(userDirectory.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        AcceptResult result = service.accept(token);

        assertThat(result.requiresRegistration()).isTrue();
        verify(groupMemberRepository, never()).addMember(any(), any(), any());
        verify(invitationRepository, never()).update(any());
    }

    // --- claimFor ---

    @Test
    void claimFor_addsToAllPendingGroups_andMarksAccepted() {
        UUID userId = UUID.randomUUID();
        UUID otherGroupId = UUID.randomUUID();
        Invitation i1 = new Invitation(UUID.randomUUID(), groupId, "x@example.com", inviterId,
                UUID.randomUUID(), InvitationStatus.PENDING, now.plusDays(3), now);
        Invitation i2 = new Invitation(UUID.randomUUID(), otherGroupId, "x@example.com", inviterId,
                UUID.randomUUID(), InvitationStatus.PENDING, now.plusDays(3), now);
        when(invitationRepository.findByEmailAndStatus("x@example.com", InvitationStatus.PENDING))
                .thenReturn(List.of(i1, i2));
        when(groupMemberRepository.isMember(any(), eq(userId))).thenReturn(false);

        service.claimFor(userId, "X@Example.com");

        verify(groupMemberRepository).addMember(groupId, userId, MemberRole.MEMBER);
        verify(groupMemberRepository).addMember(otherGroupId, userId, MemberRole.MEMBER);
        verify(invitationRepository, org.mockito.Mockito.times(2)).update(any());
    }

    @Test
    void claimFor_expiredPending_isMarkedExpired_andNotJoined() {
        UUID userId = UUID.randomUUID();
        Invitation expired = new Invitation(UUID.randomUUID(), groupId, "x@example.com", inviterId,
                UUID.randomUUID(), InvitationStatus.PENDING, now.minusDays(1), now);
        when(invitationRepository.findByEmailAndStatus("x@example.com", InvitationStatus.PENDING))
                .thenReturn(List.of(expired));

        service.claimFor(userId, "x@example.com");

        verify(groupMemberRepository, never()).addMember(any(), any(), any());
        ArgumentCaptor<Invitation> captor = ArgumentCaptor.forClass(Invitation.class);
        verify(invitationRepository).update(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(InvitationStatus.EXPIRED);
    }
}
