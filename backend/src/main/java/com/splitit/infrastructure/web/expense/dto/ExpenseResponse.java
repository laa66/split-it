package com.splitit.infrastructure.web.expense.dto;

import com.splitit.domain.expense.model.ExpenseWithShares;
import com.splitit.domain.expense.model.SplitType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ExpenseResponse(
        UUID id,
        UUID groupId,
        UUID paidBy,
        String title,
        BigDecimal amount,
        SplitType splitType,
        LocalDate expenseDate,
        OffsetDateTime createdAt,
        List<ExpenseShareResponse> shares
) {
    public static ExpenseResponse from(ExpenseWithShares ews) {
        List<ExpenseShareResponse> shares = ews.getShares().stream()
                .map(ExpenseShareResponse::from)
                .toList();
        return new ExpenseResponse(
                ews.getExpense().getId(),
                ews.getExpense().getGroupId(),
                ews.getExpense().getPaidBy(),
                ews.getExpense().getTitle(),
                ews.getExpense().getAmount(),
                ews.getExpense().getSplitType(),
                ews.getExpense().getExpenseDate(),
                ews.getExpense().getCreatedAt(),
                shares
        );
    }
}
