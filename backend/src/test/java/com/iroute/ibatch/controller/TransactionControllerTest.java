package com.iroute.ibatch.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.iroute.ibatch.application.usecase.TransactionReprocessService;
import com.iroute.ibatch.dto.response.ReprocessTransactionResponse;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionReprocessService transactionReprocessService;

    @Test
    void shouldReprocessRejectedTransactionAmount() throws Exception {
        var response = new ReprocessTransactionResponse(
                10L,
                1L,
                "PROCESADO",
                "Transaccion reprocesada correctamente");

        when(transactionReprocessService.reprocessAmount(10L, new BigDecimal("125.50"))).thenReturn(response);

        mockMvc.perform(post("/transactions/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":125.50}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value(10))
                .andExpect(jsonPath("$.fileId").value(1))
                .andExpect(jsonPath("$.status").value("PROCESADO"))
                .andExpect(jsonPath("$.message").value("Transaccion reprocesada correctamente"));
    }

    @Test
    void shouldRejectMissingAmount() throws Exception {
        mockMvc.perform(post("/transactions/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El monto es obligatorio"));
    }
}
