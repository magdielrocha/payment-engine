package paymentengine.controller;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import paymentengine.domain.Account;
import paymentengine.domain.Transaction;
import paymentengine.domain.TransactionStatus;
import paymentengine.dto.TransferRequestDTO;
import paymentengine.exception.InsufficientBalanceException;
import paymentengine.service.TransferService;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransferController.class)
public class TransferControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TransferService transferService;

    @Test
    @DisplayName("Should return 201 Created when transfer request is valid")
    void shouldReturn201WhenValidRequest() throws Exception {

        // Arrange: Prepara o DTO de entrada e a Transação de saída
        TransferRequestDTO requestDTO = new TransferRequestDTO(1L, 2L, new BigDecimal("100.00"));

        Account source = new Account(); source.setId(1L);
        Account destination = new Account(); destination.setId(2L);

        Transaction mockTransaction = new Transaction();
        mockTransaction.setId(10L);
        mockTransaction.setSourceAccount(source);
        mockTransaction.setDestinationAccount(destination);
        mockTransaction.setAmount(new BigDecimal("100.00"));
        mockTransaction.setStatus(TransactionStatus.SUCCESS);
        mockTransaction.setCreatedAt(LocalDateTime.now());

        // Ensina o mock: "Quando o controller chamar o service, devolva essa transação de mentira"
        when(transferService.executeTransfer(1L, 2L, new BigDecimal("100.00")))
                .thenReturn(mockTransaction);

        // Act & Assert: Dispara o POST e verifica o JSON de resposta
        mockMvc.perform(post("/api/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated()) // Espera o HTTP 201
                .andExpect(jsonPath("$.transactionId").value(10L))
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when amount is negative (Validation Error)")
    void shouldReturn400WhenAmountIsNegative() throws Exception {

        // Arrange: Valor negativo para forçar o erro do @Valid
        TransferRequestDTO requestDTO = new TransferRequestDTO(1L, 2L, new BigDecimal("-50.00"));

        // Act & Assert: O Spring deve barrar antes mesmo de chamar o Service
        mockMvc.perform(post("/api/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isBadRequest()) // Espera o HTTP 400
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    @DisplayName("Should return 422 Unprocessable Entity when service throws InsufficientBalanceException")
    void shouldReturn422WhenInsufficientBalance() throws Exception {

        // Arrange: DTO válido, mas o service vai recusar
        TransferRequestDTO requestDTO = new TransferRequestDTO(1L, 2L, new BigDecimal("5000.00"));

        when(transferService.executeTransfer(1L, 2L, new BigDecimal("5000.00")))
                .thenThrow(new InsufficientBalanceException("Insufficient balance"));

        // Act & Assert: Verifica se o GlobalExceptionHandler pegou o erro e formatou como esperado
        mockMvc.perform(post("/api/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().is(422)) // Espera o HTTP 422
                .andExpect(jsonPath("$.error").value("Unprocessable Entity"));
    }
}
