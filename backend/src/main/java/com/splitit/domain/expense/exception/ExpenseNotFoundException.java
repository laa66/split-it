package com.splitit.domain.expense.exception;

import java.util.UUID;

/**
 * Thrown when an expense does not exist OR the requester is not a member of its group.
 * Returns 404 to avoid leaking existence.
 */
public class ExpenseNotFoundException extends RuntimeException {

    public ExpenseNotFoundException(UUID expenseId) {
        super("Expense not found: " + expenseId);
    }
}
