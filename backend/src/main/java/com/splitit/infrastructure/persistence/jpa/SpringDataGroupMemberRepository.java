package com.splitit.infrastructure.persistence.jpa;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataGroupMemberRepository
        extends JpaRepository<GroupMemberJpaEntity, UUID> {

    boolean existsByGroupIdAndUserId(UUID groupId, UUID userId);

    @Query(value = """
            SELECT u.id AS userId, u.display_name AS displayName, u.email AS email, gm.role AS role
            FROM group_members gm
            JOIN users u ON u.id = gm.user_id
            WHERE gm.group_id = :groupId
            ORDER BY CASE gm.role WHEN 'OWNER' THEN 0 ELSE 1 END, u.display_name
            """, nativeQuery = true)
    List<GroupMemberView> findMembersByGroupId(@Param("groupId") UUID groupId);

    @Query(value = """
            SELECT g.id AS id, g.name AS name, g.description AS description,
                   mine.role AS currentUserRole, g.created_at AS createdAt,
                   (SELECT COUNT(*) FROM group_members c WHERE c.group_id = g.id) AS membersCount
            FROM group_members mine
            JOIN groups g ON g.id = mine.group_id
            WHERE mine.user_id = :userId
            ORDER BY g.created_at DESC
            """, nativeQuery = true)
    List<GroupSummaryView> findGroupsForUser(@Param("userId") UUID userId);
}
