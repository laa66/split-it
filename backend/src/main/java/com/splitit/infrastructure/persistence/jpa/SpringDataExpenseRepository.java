package com.splitit.infrastructure.persistence.jpa;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataExpenseRepository extends JpaRepository<ExpenseJpaEntity, UUID> {

    @Query(value = """
            SELECT COUNT(*) FROM expenses WHERE group_id = :groupId
            """, nativeQuery = true)
    long countByGroupId(@Param("groupId") UUID groupId);

    @Query(value = """
            SELECT * FROM expenses
            WHERE group_id = :groupId
            ORDER BY expense_date DESC, created_at DESC
            LIMIT :size OFFSET :offset
            """, nativeQuery = true)
    List<ExpenseJpaEntity> findByGroupIdPaged(
            @Param("groupId") UUID groupId,
            @Param("size") int size,
            @Param("offset") long offset);

    @Query(value = """
            SELECT e.expense_date AS expenseDate, e.title AS title,
                   u.display_name AS paidByName, e.amount AS amount, e.split_type AS splitType
            FROM expenses e
            JOIN users u ON u.id = e.paid_by
            WHERE e.group_id = :groupId
              AND e.expense_date BETWEEN :from AND :to
            ORDER BY e.expense_date DESC, e.created_at DESC
            """, nativeQuery = true)
    List<ReportExpenseView> findExpensesInRange(
            @Param("groupId") UUID groupId,
            @Param("from") java.time.LocalDate from,
            @Param("to") java.time.LocalDate to);

    @Query(value = """
            SELECT u.id AS userId, u.display_name AS displayName,
                   COALESCE(paid.total, 0) - COALESCE(owed.total, 0)
                   + COALESCE(settled_paid.total, 0) - COALESCE(settled_received.total, 0)
                   AS balance
            FROM group_members gm
            JOIN users u ON u.id = gm.user_id
            LEFT JOIN (
                SELECT paid_by AS uid, SUM(amount) AS total
                FROM expenses
                WHERE group_id = :groupId
                GROUP BY paid_by
            ) paid ON paid.uid = gm.user_id
            LEFT JOIN (
                SELECT es.user_id AS uid, SUM(es.share_amount) AS total
                FROM expense_shares es
                JOIN expenses e ON e.id = es.expense_id
                WHERE e.group_id = :groupId
                GROUP BY es.user_id
            ) owed ON owed.uid = gm.user_id
            LEFT JOIN (
                SELECT payer_id AS uid, SUM(amount) AS total
                FROM settlements
                WHERE group_id = :groupId AND status = 'CONFIRMED'
                GROUP BY payer_id
            ) settled_paid ON settled_paid.uid = gm.user_id
            LEFT JOIN (
                SELECT payee_id AS uid, SUM(amount) AS total
                FROM settlements
                WHERE group_id = :groupId AND status = 'CONFIRMED'
                GROUP BY payee_id
            ) settled_received ON settled_received.uid = gm.user_id
            WHERE gm.group_id = :groupId
            ORDER BY u.display_name
            """, nativeQuery = true)
    List<BalanceView> calculateBalances(@Param("groupId") UUID groupId);
}
