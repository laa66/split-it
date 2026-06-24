package com.splitit.domain.expense.model;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Live-calculated balance for one group member.
 * Positive = the group owes them money; negative = they owe the group.
 */
public final class MemberBalance {

    private final UUID userId;
    private final String displayName;
    private final BigDecimal balance;

    public MemberBalance(UUID userId, String displayName, BigDecimal balance) {
        this.userId = userId;
        this.displayName = displayName;
        this.balance = balance;
    }

    public UUID getUserId() { return userId; }
    public String getDisplayName() { return displayName; }
    public BigDecimal getBalance() { return balance; }
}
