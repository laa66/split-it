package com.splitit.domain.expense.port.out;

import com.splitit.domain.expense.model.Expense;
import com.splitit.domain.expense.model.ExpenseShare;
import com.splitit.domain.expense.model.ExpenseWithShares;
import com.splitit.domain.expense.model.MemberBalance;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExpenseRepository {

    /** Saves the expense and its shares atomically. Returns the persisted aggregate with id/createdAt. */
    ExpenseWithShares save(Expense expense, List<ExpenseShare> shares);

    Optional<ExpenseWithShares> findById(UUID expenseId);

    /** Returns paginated expenses for a group, ordered by expense_date DESC, then created_at DESC. */
    ExpensePage findByGroup(UUID groupId, int page, int size);

    void delete(UUID expenseId);

    /**
     * Calculates per-member balance for a group.
     * balance = Σ(paid_by == user) - Σ(share_amount of user in expense_shares)
     *           + Σ(settlement.amount where payer == user AND status == CONFIRMED)
     *           - Σ(settlement.amount where payee == user AND status == CONFIRMED)
     */
    List<MemberBalance> calculateBalances(UUID groupId);

    record ExpensePage(
            List<ExpenseWithShares> content,
            long totalElements
    ) {}
}
