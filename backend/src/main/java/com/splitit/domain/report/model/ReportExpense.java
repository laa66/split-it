package com.splitit.domain.report.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReportExpense(LocalDate date, String title, String paidByName, BigDecimal amount,
                            String splitType) {
}
