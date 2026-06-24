package com.splitit.infrastructure.web.report;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.splitit.domain.group.exception.GroupNotFoundException;
import com.splitit.domain.report.model.GroupReport;
import com.splitit.domain.report.port.in.GenerateReportUseCase;
import com.splitit.infrastructure.report.PdfReportService;
import com.splitit.infrastructure.security.AuthenticatedUser;
import com.splitit.infrastructure.security.JwtAuthenticationEntryPoint;
import com.splitit.infrastructure.security.JwtAuthenticationFilter;
import com.splitit.infrastructure.security.JwtTokenProvider;
import com.splitit.infrastructure.security.SecurityConfig;
import com.splitit.infrastructure.web.shared.GlobalExceptionHandler;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

@WebMvcTest(controllers = ReportController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class,
        GlobalExceptionHandler.class})
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GenerateReportUseCase generateReportUseCase;

    @MockBean
    private PdfReportService pdfReportService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private final UUID GROUP_ID = UUID.randomUUID();
    private final UUID CALLER_ID = UUID.randomUUID();

    private Authentication principal() {
        AuthenticatedUser user = new AuthenticatedUser(CALLER_ID, "alice@example.com");
        return new UsernamePasswordAuthenticationToken(user, null, List.of());
    }

    @Test
    void returns_401_without_token() throws Exception {
        mockMvc.perform(get("/api/groups/{groupId}/report", GROUP_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void returns_200_with_pdf_content_type_for_member() throws Exception {
        GroupReport report = new GroupReport(
                "Vacation", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30),
                List.of(), List.of(), List.of());
        byte[] fakePdf = "%PDF-fake".getBytes();

        when(generateReportUseCase.generate(eq(CALLER_ID), eq(GROUP_ID), isNull(), isNull()))
                .thenReturn(report);
        when(pdfReportService.render(report)).thenReturn(fakePdf);

        mockMvc.perform(get("/api/groups/{groupId}/report", GROUP_ID).with(authentication(principal())))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("Vacation")));
    }

    @Test
    void returns_404_for_non_member() throws Exception {
        when(generateReportUseCase.generate(any(), eq(GROUP_ID), any(), any()))
                .thenThrow(new GroupNotFoundException(GROUP_ID));

        mockMvc.perform(get("/api/groups/{groupId}/report", GROUP_ID).with(authentication(principal())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void passes_date_params_to_use_case() throws Exception {
        GroupReport report = new GroupReport(
                "G", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31),
                List.of(), List.of(), List.of());
        byte[] fakePdf = "%PDF-fake".getBytes();

        when(generateReportUseCase.generate(
                eq(CALLER_ID), eq(GROUP_ID),
                eq(LocalDate.of(2026, 1, 1)), eq(LocalDate.of(2026, 3, 31))))
                .thenReturn(report);
        when(pdfReportService.render(any())).thenReturn(fakePdf);

        mockMvc.perform(get("/api/groups/{groupId}/report", GROUP_ID)
                        .with(authentication(principal()))
                        .param("from", "2026-01-01")
                        .param("to", "2026-03-31"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"));
    }

    @Test
    void content_disposition_filename_sanitises_special_characters() throws Exception {
        // groupName with CRLF, quotes and path chars must not leak into Content-Disposition
        GroupReport report = new GroupReport(
                "My Group / \"A\"\r\nInjected",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30),
                List.of(), List.of(), List.of());
        byte[] fakePdf = "%PDF-fake".getBytes();

        when(generateReportUseCase.generate(eq(CALLER_ID), eq(GROUP_ID), isNull(), isNull()))
                .thenReturn(report);
        when(pdfReportService.render(report)).thenReturn(fakePdf);

        mockMvc.perform(get("/api/groups/{groupId}/report", GROUP_ID).with(authentication(principal())))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("\r"))))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("\n"))))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("filename=\"report-My_Group___")));
    }

    @Test
    void returns_400_when_from_is_after_to() throws Exception {
        when(generateReportUseCase.generate(any(), eq(GROUP_ID), any(), any()))
                .thenThrow(new IllegalArgumentException("'from' date must not be after 'to' date"));

        mockMvc.perform(get("/api/groups/{groupId}/report", GROUP_ID)
                        .with(authentication(principal()))
                        .param("from", "2026-06-30")
                        .param("to", "2026-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
