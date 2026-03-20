package paymentengine.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import paymentengine.domain.Account;
import paymentengine.domain.AccountStatus;
import paymentengine.dto.TransferRequestDTO;
import paymentengine.repository.AccountRepository;
import paymentengine.repository.TransactionRepository;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class TransferIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private Account sourceAccount;
    private Account destinationAccount;

    @BeforeEach
    void setUp() {
        // Limpa o banco H2 antes de cada teste para evitar sujeira
        transactionRepository.deleteAll();
        accountRepository.deleteAll();

        // Prepara o cenário de dados reais no banco H2
        sourceAccount = new Account();
        sourceAccount.setBalance(new BigDecimal("1000.00"));
        sourceAccount.setStatus(AccountStatus.ACTIVE);
        sourceAccount.setTaxId("111.111.111-11");
        sourceAccount = accountRepository.save(sourceAccount);

        destinationAccount = new Account();
        destinationAccount.setBalance(new BigDecimal("500.00"));
        destinationAccount.setStatus(AccountStatus.ACTIVE);
        destinationAccount.setTaxId("222.222.222-22");
        destinationAccount = accountRepository.save(destinationAccount);
    }

    @Test
    @DisplayName("Integration: Should process transfer and update balances in database")
    void shouldProcessTransferAndSaveToDatabase() throws Exception {
        // Arrange: Prepara o DTO com os IDs reais gerados pelo H2
        TransferRequestDTO requestDTO = new TransferRequestDTO(
                sourceAccount.getId(),
                destinationAccount.getId(),
                new BigDecimal("200.00")
        );

        // Act: Faz a chamada HTTP POST
        mockMvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().is(201)) // Espera HTTP 201
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        // Assert: Vai direto no banco de dados conferir se o dinheiro realmente mudou de lugar
        Account updatedSource = accountRepository.findById(sourceAccount.getId()).orElseThrow();
        Account updatedDestination = accountRepository.findById(destinationAccount.getId()).orElseThrow();

        assertEquals(0, new BigDecimal("800.00").compareTo(updatedSource.getBalance()));
        assertEquals(0, new BigDecimal("700.00").compareTo(updatedDestination.getBalance()));

        // Confirma se a transação ficou registrada na tabela de histórico
        assertEquals(1, transactionRepository.count());
    }

    @Test
    @DisplayName("Integration: Should rollback and not change balances if insufficient funds")
    void shouldRollbackWhenInsufficientFunds() throws Exception {
        // Arrange: Tenta transferir mais do que tem (1500 contra 1000 de saldo)
        TransferRequestDTO requestDTO = new TransferRequestDTO(
                sourceAccount.getId(),
                destinationAccount.getId(),
                new BigDecimal("1500.00")
        );

        // Act: Faz a chamada HTTP POST
        mockMvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().is(422)); // Espera HTTP 422

        // Assert: Vai no banco confirmar que a transação fez ROLLBACK (saldos intactos)
        Account updatedSource = accountRepository.findById(sourceAccount.getId()).orElseThrow();
        Account updatedDestination = accountRepository.findById(destinationAccount.getId()).orElseThrow();

        assertEquals(0, new BigDecimal("1000.00").compareTo(updatedSource.getBalance()));
        assertEquals(0, new BigDecimal("500.00").compareTo(updatedDestination.getBalance()));

        // Nenhuma transação de sucesso deve ter sido salva
        assertEquals(0, transactionRepository.count());
    }

    @Test
    @DisplayName("Integration: Should prevent double spending on concurrent requests (Pessimistic Lock)")
    void shouldPreventDoubleSpending() throws Exception {
        // Arrange: Simular 5 requisições
        int concurrentRequests = 5;
        ExecutorService executor = Executors.newFixedThreadPool(concurrentRequests);
        CountDownLatch latch = new CountDownLatch(concurrentRequests);

        // Contadores seguros para threads
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        TransferRequestDTO requestDTO = new TransferRequestDTO(
                sourceAccount.getId(),
                destinationAccount.getId(),
                new BigDecimal("1000.00") // Tenta transferir TUDO de uma vez
        );
        String jsonPayload = objectMapper.writeValueAsString(requestDTO);

        // Act: Dispara 5 requisições HTTP simultâneas
        try {
            for (int i = 0; i < concurrentRequests; i++) {
                executor.submit(() -> {
                    try {
                        mockMvc.perform(post("/api/transfers")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(jsonPayload))
                                .andDo(result -> {
                                    if (result.getResponse().getStatus() == 201) {
                                        successCount.incrementAndGet();
                                    } else {
                                        failCount.incrementAndGet();
                                    }
                                });
                    } catch (Exception e) {
                        failCount.incrementAndGet();
                    } finally {
                        latch.countDown(); // thread terminou
                    }
                });
            }
            // Espera todas as 5 requisições terminarem
            latch.await();
        } finally {
            executor.shutdown();
        }

        // Assert: Vai no banco de dados conferir a verdade absoluta
        Account updatedSource = accountRepository.findById(sourceAccount.getId()).orElseThrow();
        Account updatedDestination = accountRepository.findById(destinationAccount.getId()).orElseThrow();

        // Apenas uma requisição deve ter sucesso. As outras 4 devem falhar por saldo insuficiente
        assertEquals(1, successCount.get(), "Only one transfer should be approved");
        assertEquals(4, failCount.get(), "Four transfers should be blocked");

        assertEquals(0, new BigDecimal("0.00").compareTo(updatedSource.getBalance()));
        assertEquals(0, new BigDecimal("1500.00").compareTo(updatedDestination.getBalance()));

        // Apenas 1 registro de sucesso deve existir
        assertEquals(1, transactionRepository.count());
    }
}
