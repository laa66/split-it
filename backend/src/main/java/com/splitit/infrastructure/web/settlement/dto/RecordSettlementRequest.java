package com.splitit.infrastructure.web.settlement.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

public record RecordSettlementRequest(
        @NotNull UUID payerId,
        @NotNull UUID payeeId,
        @NotNull @Positive BigDecimal amount
) {}
