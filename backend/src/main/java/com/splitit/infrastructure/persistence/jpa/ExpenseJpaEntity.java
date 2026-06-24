package com.splitit.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "expenses")
public class ExpenseJpaEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "group_id", nullable = false, updatable = false)
    private UUID groupId;

    @Column(name = "paid_by", nullable = false, updatable = false)
    private UUID paidBy;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "split_type", nullable = false, length = 20)
    private String splitType;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected ExpenseJpaEntity() {}

    public ExpenseJpaEntity(UUID id, UUID groupId, UUID paidBy, String title, BigDecimal amount,
                             String splitType, LocalDate expenseDate, OffsetDateTime createdAt) {
        this.id = id;
        this.groupId = groupId;
        this.paidBy = paidBy;
        this.title = title;
        this.amount = amount;
        this.splitType = splitType;
        this.expenseDate = expenseDate;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getGroupId() { return groupId; }
    public UUID getPaidBy() { return paidBy; }
    public String getTitle() { return title; }
    public BigDecimal getAmount() { return amount; }
    public String getSplitType() { return splitType; }
    public LocalDate getExpenseDate() { return expenseDate; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
