package com.splitit.domain.group.service;

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
import com.splitit.domain.group.port.in.AcceptInvitationUseCase;
import com.splitit.domain.group.port.in.ClaimPendingInvitationsUseCase;
import com.splitit.domain.group.port.in.InviteToGroupUseCase;
import com.splitit.domain.group.port.out.EmailSender;
import com.splitit.domain.group.port.out.GroupMemberRepository;
import com.splitit.domain.group.port.out.GroupRepository;
import com.splitit.domain.group.port.out.InvitationRepository;
import com.splitit.domain.group.port.out.UserDirectory;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Pure domain service implementing the invitation lifecycle (scenariusz 3.7).
 * Depends only on out-ports and a Clock (injected for deterministic expiry handling).
 * Builds registration links from a configured base URL, kept as a plain String so the
 * domain stays framework-free; the bean wiring supplies the value.
 */
public class InvitationService
        implements InviteToGroupUseCase, AcceptInvitationUseCase, ClaimPendingInvitationsUseCase {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final InvitationRepository invitationRepository;
    private final UserDirectory userDirectory;
    private final EmailSender emailSender;
    private final Duration invitationTtl;
    private final String baseUrl;
    private final Clock clock;

    public InvitationService(GroupRepository groupRepository,
                             GroupMemberRepository groupMemberRepository,
                             InvitationRepository invitationRepository,
                             UserDirectory userDirectory,
                             EmailSender emailSender,
                             Duration invitationTtl,
                             String baseUrl,
                             Clock clock) {
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.invitationRepository = invitationRepository;
        this.userDirectory = userDirectory;
        this.emailSender = emailSender;
        this.invitationTtl = invitationTtl;
        this.baseUrl = baseUrl;
        this.clock = clock;
    }

    @Override
    public InviteResult invite(UUID inviterId, UUID groupId, String email) {
        if (!groupMemberRepository.isMember(groupId, inviterId)) {
            throw new GroupNotFoundException(groupId);
        }
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new GroupNotFoundException(groupId));

        String normalizedEmail = normalizeEmail(email);
        Optional<DirectoryUser> existing = userDirectory.findByEmail(normalizedEmail);

        if (existing.isPresent()) {
            DirectoryUser user = existing.get();
            if (groupMemberRepository.isMember(groupId, user.id())) {
                throw new AlreadyMemberException(normalizedEmail);
            }
            groupMemberRepository.addMember(groupId, user.id(), MemberRole.MEMBER);
            // Record an ACCEPTED invitation for audit trail, then notify the user.
            Invitation accepted = invitationRepository
                    .save(Invitation.newPending(groupId, normalizedEmail, inviterId, expiry()))
                    .withStatus(InvitationStatus.ACCEPTED);
            invitationRepository.update(accepted);
            emailSender.sendAddedToGroup(normalizedEmail, group.getName());
            return new InviteResult(true);
        }

        // Idempotency: a re-invite to a still-active PENDING invitation must not create a
        // second row or send a second email. An expired one is treated as absent: it is
        // marked EXPIRED and a fresh invitation is issued below.
        Optional<Invitation> existingPending =
                invitationRepository.findActivePending(groupId, normalizedEmail);
        if (existingPending.isPresent()) {
            Invitation pendingInvitation = existingPending.get();
            if (!pendingInvitation.isExpired(now())) {
                return new InviteResult(false);
            }
            invitationRepository.update(pendingInvitation.withStatus(InvitationStatus.EXPIRED));
        }

        Invitation pending = invitationRepository
                .save(Invitation.newPending(groupId, normalizedEmail, inviterId, expiry()));
        emailSender.sendGroupInvitation(normalizedEmail, group.getName(),
                registrationLink(pending.getToken()));
        return new InviteResult(false);
    }

    @Override
    public AcceptResult accept(UUID token) {
        Invitation invitation = invitationRepository.findByToken(token)
                .orElseThrow(InvitationNotFoundException::new);

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new InvitationNotPendingException(invitation.getStatus());
        }
        if (invitation.isExpired(now())) {
            invitationRepository.update(invitation.withStatus(InvitationStatus.EXPIRED));
            throw new InvitationExpiredException();
        }

        Group group = groupRepository.findById(invitation.getGroupId())
                .orElseThrow(() -> new GroupNotFoundException(invitation.getGroupId()));

        Optional<DirectoryUser> user = userDirectory.findByEmail(invitation.getInvitedEmail());
        if (user.isEmpty()) {
            return new AcceptResult(group.getId(), group.getName(), invitation.getInvitedEmail(), true);
        }

        if (!groupMemberRepository.isMember(group.getId(), user.get().id())) {
            groupMemberRepository.addMember(group.getId(), user.get().id(), MemberRole.MEMBER);
        }
        invitationRepository.update(invitation.withStatus(InvitationStatus.ACCEPTED));
        return new AcceptResult(group.getId(), group.getName(), invitation.getInvitedEmail(), false);
    }

    @Override
    public void claimFor(UUID userId, String email) {
        String normalizedEmail = normalizeEmail(email);
        for (Invitation invitation :
                invitationRepository.findByEmailAndStatus(normalizedEmail, InvitationStatus.PENDING)) {
            if (invitation.isExpired(now())) {
                invitationRepository.update(invitation.withStatus(InvitationStatus.EXPIRED));
                continue;
            }
            if (!groupMemberRepository.isMember(invitation.getGroupId(), userId)) {
                groupMemberRepository.addMember(invitation.getGroupId(), userId, MemberRole.MEMBER);
            }
            invitationRepository.update(invitation.withStatus(InvitationStatus.ACCEPTED));
        }
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock);
    }

    private OffsetDateTime expiry() {
        return now().plus(invitationTtl);
    }

    private String registrationLink(UUID token) {
        return baseUrl + "/register?invitation=" + token;
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
