package com.splitit.domain.expense.model;

import java.util.List;

/** Aggregate: expense with its participant shares. */
public final class ExpenseWithShares {

    private final Expense expense;
    private final List<ExpenseShare> shares;

    public ExpenseWithShares(Expense expense, List<ExpenseShare> shares) {
        this.expense = expense;
        this.shares = List.copyOf(shares);
    }

    public Expense getExpense() { return expense; }
    public List<ExpenseShare> getShares() { return shares; }
}
