package com.iroute.ibatch.config;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import com.iroute.ibatch.application.usecase.AvailableFileService;
import com.iroute.ibatch.application.usecase.DashboardService;
import com.iroute.ibatch.application.usecase.FileProcessingService;
import com.iroute.ibatch.application.usecase.FileProgressTracker;
import com.iroute.ibatch.application.usecase.ProcessedFileService;
import com.iroute.ibatch.application.usecase.TransactionReprocessService;
import com.iroute.ibatch.controller.DashboardController;
import com.iroute.ibatch.controller.DatabaseHealthController;
import com.iroute.ibatch.controller.FileController;
import com.iroute.ibatch.controller.ProcessingLogController;
import com.iroute.ibatch.controller.TransactionController;

@WebMvcTest({
        FileController.class,
        TransactionController.class,
        DashboardController.class,
        ProcessingLogController.class,
        DatabaseHealthController.class
})
@Import({SecurityConfig.class, RateLimitFilter.class})
class RoleAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AvailableFileService availableFileService;

    @MockBean
    private FileProcessingService fileProcessingService;

    @MockBean
    private ProcessedFileService processedFileService;

    @MockBean
    private FileProgressTracker fileProgressTracker;

    @MockBean
    private TransactionReprocessService transactionReprocessService;

    @MockBean
    private DashboardService dashboardService;

    @MockBean
    private DataSource dataSource;

    @Test
    void operatorCanReadHistoryAndProcessFiles() throws Exception {
        mockMvc.perform(get("/files").with(user("operator").roles("OPERATOR")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/files/process")
                        .with(user("operator").roles("OPERATOR"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fileName\":\"transactions_15082026.csv\"}"))
                .andExpect(status().isAccepted());

        var csv = new MockMultipartFile(
                "file",
                "transactions_15082026.csv",
                "text/csv",
                "cuenta,monto,fecha\n001,25.50,2026-08-15".getBytes());

        mockMvc.perform(multipart("/files/upload")
                        .file(csv)
                        .with(user("operator").roles("OPERATOR"))
                        .with(csrf()))
                .andExpect(status().isCreated());
    }

    @Test
    void operatorCannotUseAdministrativeFunctions() throws Exception {
        mockMvc.perform(post("/transactions/1")
                        .with(user("operator").roles("OPERATOR"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":25.50}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/dashboard/summary").with(user("operator").roles("OPERATOR")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/logs").with(user("operator").roles("OPERATOR")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/health/database").with(user("operator").roles("OPERATOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanUseAdministrativeFunctions() throws Exception {
        mockMvc.perform(post("/transactions/1")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":25.50}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/dashboard/summary").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/logs").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }
}
