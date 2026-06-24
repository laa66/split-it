package com.splitit.domain.expense.exception;

/**
 * Thrown when a user attempts to delete an expense they did not create
 * and they are not the group owner.
 */
public class ExpenseAccessDeniedException extends RuntimeException {

    public ExpenseAccessDeniedException() {
        super("You do not have permission to delete this expense");
    }
}
