package com.splitit.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataInvitationRepository
        extends JpaRepository<InvitationJpaEntity, UUID> {

    Optional<InvitationJpaEntity> findByToken(UUID token);

    List<InvitationJpaEntity> findByInvitedEmailAndStatus(String invitedEmail, String status);

    @Query("""
            SELECT i FROM InvitationJpaEntity i
            WHERE i.groupId = :groupId
              AND i.invitedEmail = :invitedEmail
              AND i.status = 'PENDING'
            """)
    Optional<InvitationJpaEntity> findActivePending(@Param("groupId") UUID groupId,
                                                    @Param("invitedEmail") String invitedEmail);
}
