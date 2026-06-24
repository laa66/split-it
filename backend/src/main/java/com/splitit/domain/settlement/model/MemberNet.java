package com.splitit.domain.settlement.model;

import java.math.BigDecimal;
import java.util.UUID;

/** Net balance of a group member: positive = is owed, negative = owes. */
public class MemberNet {

    private final UUID userId;
    private final String displayName;
    private final BigDecimal balance;

    public MemberNet(UUID userId, String displayName, BigDecimal balance) {
        this.userId = userId;
        this.displayName = displayName;
        this.balance = balance;
    }

    public UUID getUserId() { return userId; }
    public String getDisplayName() { return displayName; }
    public BigDecimal getBalance() { return balance; }
}
