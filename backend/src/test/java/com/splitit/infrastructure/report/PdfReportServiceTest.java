package com.splitit.infrastructure.report;

import static org.assertj.core.api.Assertions.assertThat;

import com.splitit.domain.report.model.GroupReport;
import com.splitit.domain.report.model.ReportBalance;
import com.splitit.domain.report.model.ReportExpense;
import com.splitit.domain.report.model.ReportSettlement;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class PdfReportServiceTest {

    private final PdfReportService service = new PdfReportService();

    @Test
    void render_returns_non_empty_bytes_starting_with_pdf_signature() {
        GroupReport report = new GroupReport(
                "Test Group",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 6, 30),
                List.of(new ReportExpense(
                        LocalDate.of(2026, 3, 10), "Lunch", "Alice", new BigDecimal("60.00"), "EQUAL")),
                List.of(new ReportBalance("Alice", new BigDecimal("30.00")),
                        new ReportBalance("Bob", new BigDecimal("-30.00"))),
                List.of(new ReportSettlement("Bob", "Alice", new BigDecimal("30.00"))));

        byte[] pdf = service.render(report);

        assertThat(pdf).isNotEmpty();
        // PDF magic bytes: %PDF
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
    }

    @Test
    void render_empty_report_still_produces_valid_pdf() {
        GroupReport report = new GroupReport(
                "Empty Group",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 6, 30),
                List.of(),
                List.of(),
                List.of());

        byte[] pdf = service.render(report);

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
    }
}
