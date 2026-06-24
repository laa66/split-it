package com.splitit.domain.report.model;

import java.math.BigDecimal;

public record ReportSettlement(String payerName, String payeeName, BigDecimal amount) {
}
