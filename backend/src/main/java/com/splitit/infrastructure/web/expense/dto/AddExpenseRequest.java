package com.splitit.infrastructure.web.expense.dto;

import com.splitit.domain.expense.model.SplitType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AddExpenseRequest(
        @NotBlank @Size(max = 200) String title,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        @NotNull UUID paidBy,
        @NotNull SplitType splitType,
        @NotNull LocalDate expenseDate,
        @NotEmpty @Valid List<ParticipantShareRequest> participants
) {
    public record ParticipantShareRequest(
            @NotNull UUID userId,
            BigDecimal value
    ) {}
}
