package com.splitit.infrastructure.web.expense.dto;

import com.splitit.domain.expense.port.in.ManageExpensesUseCase.ExpensePage;
import java.util.List;

public record ExpensePageResponse(
        List<ExpenseResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static ExpensePageResponse from(ExpensePage p) {
        List<ExpenseResponse> content = p.content().stream()
                .map(ExpenseResponse::from)
                .toList();
        return new ExpensePageResponse(content, p.page(), p.size(), p.totalElements(), p.totalPages());
    }
}
