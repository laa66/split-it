package com.splitit.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "settlements")
public class SettlementJpaEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "group_id", nullable = false, updatable = false)
    private UUID groupId;

    @Column(name = "payer_id", nullable = false, updatable = false)
    private UUID payerId;

    @Column(name = "payee_id", nullable = false, updatable = false)
    private UUID payeeId;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "status", nullable = false, length = 10)
    private String status;

    @Column(name = "confirmed_by_payer", nullable = false)
    private boolean confirmedByPayer;

    @Column(name = "confirmed_by_payee", nullable = false)
    private boolean confirmedByPayee;

    @Column(name = "settled_at")
    private OffsetDateTime settledAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected SettlementJpaEntity() {}

    public SettlementJpaEntity(UUID id, UUID groupId, UUID payerId, UUID payeeId,
                                BigDecimal amount, String status,
                                boolean confirmedByPayer, boolean confirmedByPayee,
                                OffsetDateTime settledAt, OffsetDateTime createdAt) {
        this.id = id;
        this.groupId = groupId;
        this.payerId = payerId;
        this.payeeId = payeeId;
        this.amount = amount;
        this.status = status;
        this.confirmedByPayer = confirmedByPayer;
        this.confirmedByPayee = confirmedByPayee;
        this.settledAt = settledAt;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getGroupId() { return groupId; }
    public UUID getPayerId() { return payerId; }
    public UUID getPayeeId() { return payeeId; }
    public BigDecimal getAmount() { return amount; }
    public String getStatus() { return status; }
    public boolean isConfirmedByPayer() { return confirmedByPayer; }
    public boolean isConfirmedByPayee() { return confirmedByPayee; }
    public OffsetDateTime getSettledAt() { return settledAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
