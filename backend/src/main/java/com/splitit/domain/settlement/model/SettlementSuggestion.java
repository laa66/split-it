package com.splitit.domain.settlement.model;

import java.math.BigDecimal;
import java.util.UUID;

public class SettlementSuggestion {

    private final UUID payerId;
    private final String payerName;
    private final UUID payeeId;
    private final String payeeName;
    private final BigDecimal amount;

    public SettlementSuggestion(UUID payerId, String payerName,
                                UUID payeeId, String payeeName,
                                BigDecimal amount) {
        this.payerId = payerId;
        this.payerName = payerName;
        this.payeeId = payeeId;
        this.payeeName = payeeName;
        this.amount = amount;
    }

    public UUID getPayerId() { return payerId; }
    public String getPayerName() { return payerName; }
    public UUID getPayeeId() { return payeeId; }
    public String getPayeeName() { return payeeName; }
    public BigDecimal getAmount() { return amount; }
}
