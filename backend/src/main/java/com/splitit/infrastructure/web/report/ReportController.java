package com.splitit.infrastructure.web.report;

import com.splitit.domain.report.model.GroupReport;
import com.splitit.domain.report.port.in.GenerateReportUseCase;
import com.splitit.infrastructure.report.PdfReportService;
import com.splitit.infrastructure.security.AuthenticatedUser;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/groups/{groupId}/report")
public class ReportController {

    private final GenerateReportUseCase generateReportUseCase;
    private final PdfReportService pdfReportService;

    public ReportController(GenerateReportUseCase generateReportUseCase,
                            PdfReportService pdfReportService) {
        this.generateReportUseCase = generateReportUseCase;
        this.pdfReportService = pdfReportService;
    }

    @GetMapping
    public ResponseEntity<byte[]> getReport(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID groupId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        GroupReport report = generateReportUseCase.generate(user.id(), groupId, from, to);
        byte[] pdf = pdfReportService.render(report);

        String filename = "report-" + report.groupName().replaceAll("[^a-zA-Z0-9\\-_]", "_") + ".pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(pdf);
    }
}
