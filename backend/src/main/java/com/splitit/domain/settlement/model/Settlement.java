package com.splitit.domain.settlement.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public class Settlement {

    private final UUID id;
    private final UUID groupId;
    private final UUID payerId;
    private final UUID payeeId;
    private final BigDecimal amount;
    private final String status;
    private final OffsetDateTime settledAt;
    private final OffsetDateTime createdAt;

    public Settlement(UUID id, UUID groupId, UUID payerId, UUID payeeId,
                      BigDecimal amount, String status,
                      OffsetDateTime settledAt, OffsetDateTime createdAt) {
        this.id = id;
        this.groupId = groupId;
        this.payerId = payerId;
        this.payeeId = payeeId;
        this.amount = amount;
        this.status = status;
        this.settledAt = settledAt;
        this.createdAt = createdAt;
    }

    /** Factory: creates a settlement that is immediately confirmed (mark-paid model). */
    public static Settlement confirmed(UUID groupId, UUID payerId, UUID payeeId, BigDecimal amount) {
        OffsetDateTime now = OffsetDateTime.now(java.time.ZoneOffset.UTC);
        return new Settlement(null, groupId, payerId, payeeId, amount, "CONFIRMED", now, now);
    }

    public UUID getId() { return id; }
    public UUID getGroupId() { return groupId; }
    public UUID getPayerId() { return payerId; }
    public UUID getPayeeId() { return payeeId; }
    public BigDecimal getAmount() { return amount; }
    public String getStatus() { return status; }
    public OffsetDateTime getSettledAt() { return settledAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
