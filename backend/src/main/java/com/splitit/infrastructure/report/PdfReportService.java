package com.splitit.infrastructure.report;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.splitit.domain.report.model.GroupReport;
import com.splitit.domain.report.model.ReportBalance;
import com.splitit.domain.report.model.ReportExpense;
import com.splitit.domain.report.model.ReportSettlement;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class PdfReportService {

    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
    private static final Font HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
    private static final Font SECTION_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
    private static final Font CELL_HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
    private static final Font CELL_FONT = FontFactory.getFont(FontFactory.HELVETICA, 9);
    private static final Color HEADER_BG = new Color(0x38, 0x80, 0xff);

    public byte[] render(GroupReport report) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document();
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            document.add(new Paragraph("Split-it — Group Report", TITLE_FONT));
            document.add(new Paragraph(
                    report.groupName() + "  |  " + report.from() + " – " + report.to(),
                    HEADER_FONT));
            document.add(new Paragraph(" "));

            addExpensesSection(document, report);
            addBalancesSection(document, report);
            addSettlementsSection(document, report);

        } catch (DocumentException e) {
            throw new IllegalStateException("Failed to generate PDF report", e);
        } finally {
            document.close();
        }
        return out.toByteArray();
    }

    private void addExpensesSection(Document document, GroupReport report) throws DocumentException {
        document.add(new Paragraph("Expenses", SECTION_FONT));
        document.add(new Paragraph(" "));

        if (report.expenses().isEmpty()) {
            document.add(new Paragraph("No expenses in the selected period.", CELL_FONT));
            document.add(new Paragraph(" "));
            return;
        }

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{2f, 4f, 3f, 2f, 2f});

        addTableHeader(table, "Date", "Title", "Paid by", "Amount", "Split");

        for (ReportExpense e : report.expenses()) {
            table.addCell(cell(e.date().toString()));
            table.addCell(cell(e.title()));
            table.addCell(cell(e.paidByName()));
            table.addCell(cellRight(formatAmount(e.amount())));
            table.addCell(cell(e.splitType()));
        }

        document.add(table);
        document.add(new Paragraph(" "));
    }

    private void addBalancesSection(Document document, GroupReport report) throws DocumentException {
        document.add(new Paragraph("Current Balances", SECTION_FONT));
        document.add(new Paragraph(" "));

        if (report.balances().isEmpty()) {
            document.add(new Paragraph("No members.", CELL_FONT));
            document.add(new Paragraph(" "));
            return;
        }

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(60);
        table.setWidths(new float[]{4f, 2f});

        addTableHeader(table, "Member", "Balance");

        for (ReportBalance b : report.balances()) {
            table.addCell(cell(b.displayName()));
            PdfPCell amountCell = cellRight(formatAmount(b.balance()));
            if (b.balance().compareTo(BigDecimal.ZERO) < 0) {
                amountCell.setPhrase(new Phrase(formatAmount(b.balance()),
                        FontFactory.getFont(FontFactory.HELVETICA, 9, new Color(0xcc, 0x33, 0x33))));
            } else if (b.balance().compareTo(BigDecimal.ZERO) > 0) {
                amountCell.setPhrase(new Phrase(formatAmount(b.balance()),
                        FontFactory.getFont(FontFactory.HELVETICA, 9, new Color(0x22, 0x99, 0x22))));
            }
            table.addCell(amountCell);
        }

        document.add(table);
        document.add(new Paragraph(" "));
    }

    private void addSettlementsSection(Document document, GroupReport report) throws DocumentException {
        document.add(new Paragraph("Settlement Plan", SECTION_FONT));
        document.add(new Paragraph(" "));

        if (report.settlements().isEmpty()) {
            document.add(new Paragraph("All balances are settled.", CELL_FONT));
            document.add(new Paragraph(" "));
            return;
        }

        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(80);
        table.setWidths(new float[]{3f, 3f, 2f});

        addTableHeader(table, "Payer", "Payee", "Amount");

        for (ReportSettlement s : report.settlements()) {
            table.addCell(cell(s.payerName()));
            table.addCell(cell(s.payeeName()));
            table.addCell(cellRight(formatAmount(s.amount())));
        }

        document.add(table);
    }

    private void addTableHeader(PdfPTable table, String... headers) {
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, CELL_HEADER_FONT));
            cell.setBackgroundColor(HEADER_BG);
            cell.setPadding(4);
            cell.setHorizontalAlignment(Element.ALIGN_LEFT);
            table.addCell(cell);
        }
    }

    private PdfPCell cell(String text) {
        PdfPCell c = new PdfPCell(new Phrase(text, CELL_FONT));
        c.setPadding(4);
        c.setHorizontalAlignment(Element.ALIGN_LEFT);
        return c;
    }

    private PdfPCell cellRight(String text) {
        PdfPCell c = new PdfPCell(new Phrase(text, CELL_FONT));
        c.setPadding(4);
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return c;
    }

    private String formatAmount(BigDecimal amount) {
        return String.format("%.2f", amount);
    }
}
