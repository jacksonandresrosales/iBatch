package com.iroute.ibatch.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.iroute.ibatch.dto.response.AvailableFileResponse;
import com.iroute.ibatch.dto.response.ProcessFileResponse;
import com.iroute.ibatch.infrastructure.file.InputFileService;

@WebMvcTest(FileController.class)
class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InputFileService inputFileService;

    @Test
    void shouldReturnAvailableFiles() throws Exception {
        var response = new AvailableFileResponse(
                "transactions_30072026.csv",
                1024,
                OffsetDateTime.parse("2026-07-30T18:30:00-05:00"),
                true);

        when(inputFileService.findAvailableCsvFiles()).thenReturn(List.of(response));

        mockMvc.perform(get("/files/available"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fileName").value("transactions_30072026.csv"))
                .andExpect(jsonPath("$[0].sizeBytes").value(1024))
                .andExpect(jsonPath("$[0].expectedFormat").value(true));
    }

    @Test
    void shouldValidateFileForProcessing() throws Exception {
        var response = new ProcessFileResponse(
                "transactions_30072026.csv",
                "VALIDATED",
                "Archivo validado para procesamiento");

        when(inputFileService.validateFileForProcessing("transactions_30072026.csv")).thenReturn(response);

        mockMvc.perform(post("/files/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fileName\":\"transactions_30072026.csv\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value("transactions_30072026.csv"))
                .andExpect(jsonPath("$.status").value("VALIDATED"))
                .andExpect(jsonPath("$.message").value("Archivo validado para procesamiento"));
    }

    @Test
    void shouldRejectEmptyFileName() throws Exception {
        mockMvc.perform(post("/files/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fileName\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El nombre del archivo es obligatorio"));
    }
}
