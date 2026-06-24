package com.splitit.infrastructure.web.settlement.dto;

import com.splitit.domain.settlement.model.Settlement;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record SettlementResponse(
        UUID id,
        UUID groupId,
        UUID payerId,
        UUID payeeId,
        BigDecimal amount,
        String status,
        OffsetDateTime settledAt,
        OffsetDateTime createdAt
) {
    public static SettlementResponse from(Settlement s) {
        return new SettlementResponse(
                s.getId(),
                s.getGroupId(),
                s.getPayerId(),
                s.getPayeeId(),
                s.getAmount().setScale(2),
                s.getStatus(),
                s.getSettledAt(),
                s.getCreatedAt()
        );
    }
}
