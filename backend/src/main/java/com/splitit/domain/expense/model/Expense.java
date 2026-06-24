package com.splitit.domain.expense.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Immutable expense aggregate root. Shares are stored separately in ExpenseShare. */
public final class Expense {

    private final UUID id;
    private final UUID groupId;
    private final UUID paidBy;
    private final String title;
    private final BigDecimal amount;
    private final SplitType splitType;
    private final LocalDate expenseDate;
    private final OffsetDateTime createdAt;

    public Expense(UUID id, UUID groupId, UUID paidBy, String title, BigDecimal amount,
                   SplitType splitType, LocalDate expenseDate, OffsetDateTime createdAt) {
        this.id = id;
        this.groupId = groupId;
        this.paidBy = paidBy;
        this.title = title;
        this.amount = amount;
        this.splitType = splitType;
        this.expenseDate = expenseDate;
        this.createdAt = createdAt;
    }

    /** Factory for new (unsaved) expense — id and createdAt will be assigned by DB. */
    public static Expense newExpense(UUID groupId, UUID paidBy, String title, BigDecimal amount,
                                     SplitType splitType, LocalDate expenseDate) {
        return new Expense(null, groupId, paidBy, title, amount, splitType, expenseDate, null);
    }

    public UUID getId() { return id; }
    public UUID getGroupId() { return groupId; }
    public UUID getPaidBy() { return paidBy; }
    public String getTitle() { return title; }
    public BigDecimal getAmount() { return amount; }
    public SplitType getSplitType() { return splitType; }
    public LocalDate getExpenseDate() { return expenseDate; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
