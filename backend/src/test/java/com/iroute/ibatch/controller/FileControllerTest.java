package com.iroute.ibatch.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.iroute.ibatch.application.usecase.AvailableFileService;
import com.iroute.ibatch.application.usecase.FileProcessingService;
import com.iroute.ibatch.application.usecase.FileProgressTracker;
import com.iroute.ibatch.application.usecase.ProcessedFileService;
import com.iroute.ibatch.dto.response.AvailableFileResponse;
import com.iroute.ibatch.dto.response.FileDetailResponse;
import com.iroute.ibatch.dto.response.ProcessFileResponse;
import com.iroute.ibatch.dto.response.ProcessedFileResponse;
import com.iroute.ibatch.dto.response.TransactionDetailResponse;
import com.iroute.ibatch.dto.response.TransactionRejectionResponse;

@WebMvcTest(FileController.class)
@AutoConfigureMockMvc(addFilters = false)
class FileControllerTest {

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

    @Test
    void shouldReturnProcessedFiles() throws Exception {
        var response = new ProcessedFileResponse(
                1L,
                "transactions_30072026.csv",
                "PROCESADO",
                10,
                8,
                2,
                null,
                LocalDateTime.parse("2026-07-30T18:50:00"),
                LocalDateTime.parse("2026-07-30T18:55:00"));

        when(processedFileService.findAll()).thenReturn(List.of(response));

        mockMvc.perform(get("/files"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].fileName").value("transactions_30072026.csv"))
                .andExpect(jsonPath("$[0].status").value("PROCESADO"))
                .andExpect(jsonPath("$[0].totalTransactions").value(10))
                .andExpect(jsonPath("$[0].processedTransactions").value(8))
                .andExpect(jsonPath("$[0].rejectedTransactions").value(2));
    }

    @Test
    void shouldReturnAvailableFiles() throws Exception {
        var response = new AvailableFileResponse(
                "transactions_30072026.csv",
                1024,
                OffsetDateTime.parse("2026-07-30T18:30:00-05:00"),
                true);

        when(availableFileService.findAvailableFiles()).thenReturn(List.of(response));

        mockMvc.perform(get("/files/available"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fileName").value("transactions_30072026.csv"))
                .andExpect(jsonPath("$[0].sizeBytes").value(1024))
                .andExpect(jsonPath("$[0].expectedFormat").value(true));
    }

    @Test
    void shouldReturnProcessedFileDetail() throws Exception {
        var file = new ProcessedFileResponse(
                1L,
                "transactions_30072026.csv",
                "PROCESADO_CON_RECHAZOS",
                2,
                1,
                1,
                null,
                LocalDateTime.parse("2026-07-30T18:50:00"),
                LocalDateTime.parse("2026-07-30T18:55:00"));
        var rejection = new TransactionRejectionResponse(
                100L,
                "MONTO_INVALIDO",
                "Monto invalido",
                "El monto debe ser un valor monetario valido",
                LocalDateTime.parse("2026-07-30T18:52:00"));
        var transaction = new TransactionDetailResponse(
                10L,
                2,
                "2000000000",
                "xyz",
                "31/07/2026",
                "2000000000",
                null,
                null,
                "RECHAZADA",
                List.of(rejection),
                LocalDateTime.parse("2026-07-30T18:51:00"),
                LocalDateTime.parse("2026-07-30T18:52:00"));

        when(processedFileService.findDetailById(1L, 0, 50, null, "2000000000"))
                .thenReturn(new FileDetailResponse(file, List.of(transaction), 0, 50, 1, 1));

        mockMvc.perform(get("/files/1").param("account", "2000000000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.file.id").value(1))
                .andExpect(jsonPath("$.file.status").value("PROCESADO_CON_RECHAZOS"))
                .andExpect(jsonPath("$.transactions[0].transactionId").value(10))
                .andExpect(jsonPath("$.transactions[0].status").value("RECHAZADA"))
                .andExpect(jsonPath("$.transactions[0].rejections[0].reasonCode").value("MONTO_INVALIDO"));
    }

    @Test
    void shouldValidateFileForProcessing() throws Exception {
        var response = new ProcessFileResponse(
                1L,
                "transactions_30072026.csv",
                "PROCESADO",
                "Archivo procesado correctamente",
                2,
                2,
                0);

        when(fileProcessingService.registerFileForProcessing("transactions_30072026.csv")).thenReturn(response);

        mockMvc.perform(post("/files/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fileName\":\"transactions_30072026.csv\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.fileId").value(1))
                .andExpect(jsonPath("$.fileName").value("transactions_30072026.csv"))
                .andExpect(jsonPath("$.status").value("PROCESADO"))
                .andExpect(jsonPath("$.message").value("Archivo procesado correctamente"))
                .andExpect(jsonPath("$.totalRecords").value(2))
                .andExpect(jsonPath("$.processedCount").value(2))
                .andExpect(jsonPath("$.rejectedCount").value(0));
    }

    @Test
    void shouldRejectEmptyFileName() throws Exception {
        mockMvc.perform(post("/files/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fileName\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El nombre del archivo es obligatorio"));
    }

    @Test
    void shouldRejectNonPositiveFileId() throws Exception {
        mockMvc.perform(get("/files/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La solicitud no es valida"));
    }
}
