package com.splitit.domain.expense.port.in;

import com.splitit.domain.expense.model.ExpenseWithShares;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ManageExpensesUseCase {

    /**
     * Adds an expense to the group. The caller must be a group member, paidBy must be a member,
     * and all participants must be members. Shares are calculated and persisted atomically.
     *
     * @throws com.splitit.domain.group.exception.GroupNotFoundException if not a member
     * @throws com.splitit.domain.expense.exception.InvalidSplitException on invalid split params
     */
    ExpenseWithShares addExpense(UUID callerId, UUID groupId, AddExpenseCommand command);

    /**
     * Returns paginated expenses for a group. Caller must be a member.
     *
     * @throws com.splitit.domain.group.exception.GroupNotFoundException if not a member
     */
    ExpensePage listExpenses(UUID callerId, UUID groupId, int page, int size);

    /**
     * Deletes an expense. Caller must be the creator OR the group owner.
     * Non-member or non-existent → 404. Unauthorized delete → 403.
     *
     * @throws com.splitit.domain.expense.exception.ExpenseNotFoundException if absent or not member
     * @throws com.splitit.domain.expense.exception.ExpenseAccessDeniedException if not creator/owner
     */
    void deleteExpense(UUID callerId, UUID expenseId);

    record AddExpenseCommand(
            String title,
            BigDecimal amount,
            UUID paidBy,
            com.splitit.domain.expense.model.SplitType splitType,
            LocalDate expenseDate,
            List<ParticipantShare> participants
    ) {}

    record ParticipantShare(UUID userId, BigDecimal value) {}

    record ExpensePage(
            List<ExpenseWithShares> content,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {}
}
