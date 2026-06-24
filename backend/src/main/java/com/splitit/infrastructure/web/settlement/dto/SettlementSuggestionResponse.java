package com.splitit.infrastructure.web.settlement.dto;

import com.splitit.domain.settlement.model.SettlementSuggestion;
import java.math.BigDecimal;
import java.util.UUID;

public record SettlementSuggestionResponse(
        UUID payerId,
        String payerName,
        UUID payeeId,
        String payeeName,
        BigDecimal amount
) {
    public static SettlementSuggestionResponse from(SettlementSuggestion s) {
        return new SettlementSuggestionResponse(
                s.getPayerId(), s.getPayerName(),
                s.getPayeeId(), s.getPayeeName(),
                s.getAmount().setScale(2)
        );
    }
}
