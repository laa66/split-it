package com.splitit.infrastructure.persistence.jpa;

import com.splitit.domain.group.model.Invitation;
import com.splitit.domain.group.model.InvitationStatus;
import com.splitit.domain.group.port.out.InvitationRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class InvitationRepositoryAdapter implements InvitationRepository {

    private final SpringDataInvitationRepository jpaRepository;

    public InvitationRepositoryAdapter(SpringDataInvitationRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Invitation save(Invitation invitation) {
        UUID id = invitation.getId() != null ? invitation.getId() : UUID.randomUUID();
        UUID token = invitation.getToken() != null ? invitation.getToken() : UUID.randomUUID();
        OffsetDateTime createdAt =
                invitation.getCreatedAt() != null ? invitation.getCreatedAt() : OffsetDateTime.now();

        InvitationJpaEntity entity = new InvitationJpaEntity(
                id, invitation.getGroupId(), invitation.getInvitedEmail(), invitation.getInvitedBy(),
                token, invitation.getStatus().name(), invitation.getExpiresAt(), createdAt);

        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public Invitation update(Invitation invitation) {
        InvitationJpaEntity entity = jpaRepository.findById(invitation.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Invitation to update no longer exists: " + invitation.getId()));
        entity.setStatus(invitation.getStatus().name());
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<Invitation> findByToken(UUID token) {
        return jpaRepository.findByToken(token).map(this::toDomain);
    }

    @Override
    public List<Invitation> findByEmailAndStatus(String invitedEmail, InvitationStatus status) {
        return jpaRepository.findByInvitedEmailAndStatus(invitedEmail, status.name()).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<Invitation> findActivePending(UUID groupId, String invitedEmail) {
        return jpaRepository.findActivePending(groupId, invitedEmail).map(this::toDomain);
    }

    private Invitation toDomain(InvitationJpaEntity entity) {
        return new Invitation(entity.getId(), entity.getGroupId(), entity.getInvitedEmail(),
                entity.getInvitedBy(), entity.getToken(),
                InvitationStatus.valueOf(entity.getStatus()), entity.getExpiresAt(),
                entity.getCreatedAt());
    }
}
