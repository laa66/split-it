package com.splitit.domain.expense.model;

import java.math.BigDecimal;
import java.util.UUID;

public final class ExpenseShare {

    private final UUID userId;
    private final BigDecimal shareAmount;

    public ExpenseShare(UUID userId, BigDecimal shareAmount) {
        this.userId = userId;
        this.shareAmount = shareAmount;
    }

    public UUID getUserId() { return userId; }
    public BigDecimal getShareAmount() { return shareAmount; }
}
