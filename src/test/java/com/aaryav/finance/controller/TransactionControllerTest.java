package com.aaryav.finance.controller;

import com.aaryav.finance.dto.request.TransactionRequest;
import com.aaryav.finance.dto.request.TransactionUpdateRequest;
import com.aaryav.finance.dto.response.TransactionResponse;
import com.aaryav.finance.exception.GlobalExceptionHandler;
import com.aaryav.finance.exception.ResourceNotFoundException;
import com.aaryav.finance.service.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TransactionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class TransactionControllerTest {

    private static final String BASE_PATH = "/api";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransactionService transactionService;

    @Test
    @DisplayName("Debe crear una transacción válida y responder 201 Created")
    void shouldCreateTransactionSuccessfully() throws Exception {
        // Arrange: preparar los datos y configurar el comportamiento del servicio simulado.
        TransactionRequest request = TransactionRequest.builder()
                .amount(new BigDecimal("150.00"))
                .date(LocalDate.of(2024, 9, 10))
                .category("Food")
                .description("Groceries")
                .build();
        TransactionResponse response = transactionResponse(
                1L, "150.00", LocalDate.of(2024, 9, 10), "Food", "Groceries", "EXPENSE");
        when(transactionService.createTransaction(any(TransactionRequest.class))).thenReturn(response);

        // Act: ejecutar la solicitud HTTP mediante MockMvc.
        ResultActions result = mockMvc.perform(post(BASE_PATH + "/transactions")
                .contextPath(BASE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // Assert: verificar el estado HTTP, la respuesta JSON y la interacción con el servicio.
        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.amount").value(150.0))
                .andExpect(jsonPath("$.date").value("2024-09-10"))
                .andExpect(jsonPath("$.category").value("Food"));

        ArgumentCaptor<TransactionRequest> captor = ArgumentCaptor.forClass(TransactionRequest.class);
        verify(transactionService).createTransaction(captor.capture());
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo("150.00");
        assertThat(captor.getValue().getDate()).isEqualTo(LocalDate.of(2024, 9, 10));
        assertThat(captor.getValue().getCategory()).isEqualTo("Food");
        assertThat(captor.getValue().getDescription()).isEqualTo("Groceries");
    }

    @Test
    @DisplayName("Debe rechazar una cantidad negativa en la transacción")
    void shouldRejectNegativeTransactionAmount() throws Exception {
        // Arrange: preparar los datos y configurar el comportamiento del servicio simulado.
        TransactionRequest request = TransactionRequest.builder()
                .amount(new BigDecimal("-50.00"))
                .date(LocalDate.of(2024, 9, 10))
                .category("Food")
                .description("Groceries")
                .build();

        // Act: ejecutar la solicitud HTTP mediante MockMvc.
        ResultActions result = mockMvc.perform(post(BASE_PATH + "/transactions")
                .contextPath(BASE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // Assert: verificar el estado HTTP, la respuesta JSON y la interacción con el servicio.
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Amount must be positive")));
        verifyNoInteractions(transactionService);
    }

    @Test
    @DisplayName("Debe rechazar campos obligatorios ausentes al crear una transacción")
    void shouldRejectMissingRequiredTransactionFields() throws Exception {
        // Arrange: preparar los datos y configurar el comportamiento del servicio simulado.
        TransactionRequest request = TransactionRequest.builder()
                .amount(null)
                .date(null)
                .category("")
                .description("Groceries")
                .build();

        // Act: ejecutar la solicitud HTTP mediante MockMvc.
        ResultActions result = mockMvc.perform(post(BASE_PATH + "/transactions")
                .contextPath(BASE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // Assert: verificar el estado HTTP, la respuesta JSON y la interacción con el servicio.
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Amount is required")))
                .andExpect(jsonPath("$.message").value(containsString("Date is required")))
                .andExpect(jsonPath("$.message").value(containsString("Category is required")));
        verifyNoInteractions(transactionService);
    }

    @Test
    @DisplayName("Debe obtener transacciones sin filtros con 200 OK")
    void shouldGetTransactionsWithoutFilters() throws Exception {
        // Arrange: preparar los datos y configurar el comportamiento del servicio simulado.
        Map<String, List<TransactionResponse>> response = Map.of("transactions", List.of(
                transactionResponse(1L, "150.00", LocalDate.of(2024, 8, 5),
                        "Food", "Groceries", "EXPENSE"),
                transactionResponse(2L, "2500.00", LocalDate.of(2024, 8, 15),
                        "Salary", "Monthly salary", "INCOME")));
        when(transactionService.getTransactions(null, null, null, null)).thenReturn(response);

        // Act: ejecutar la solicitud HTTP mediante MockMvc.
        ResultActions result = mockMvc.perform(get(BASE_PATH + "/transactions")
                .contextPath(BASE_PATH));

        // Assert: verificar el estado HTTP, la respuesta JSON y la interacción con el servicio.
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions[0].category").value("Food"))
                .andExpect(jsonPath("$.transactions[0].amount").value(150.0))
                .andExpect(jsonPath("$.transactions[1].category").value("Salary"));
        verify(transactionService).getTransactions(null, null, null, null);
    }

    @Test
    @DisplayName("Debe filtrar transacciones por rango de fechas y categoría")
    void shouldGetTransactionsWithFilters() throws Exception {
        // Arrange: preparar los datos y configurar el comportamiento del servicio simulado.
        LocalDate startDate = LocalDate.of(2024, 8, 1);
        LocalDate endDate = LocalDate.of(2024, 8, 31);
        Map<String, List<TransactionResponse>> response = Map.of("transactions", List.of(
                transactionResponse(1L, "150.00", LocalDate.of(2024, 8, 5),
                        "Food", "Groceries", "EXPENSE")));
        when(transactionService.getTransactions(startDate, endDate, 2L, "Food")).thenReturn(response);

        // Act: ejecutar la solicitud HTTP mediante MockMvc.
        ResultActions result = mockMvc.perform(get(BASE_PATH + "/transactions")
                .contextPath(BASE_PATH)
                .param("startDate", "2024-08-01")
                .param("endDate", "2024-08-31")
                .param("categoryId", "2")
                .param("category", "Food"));

        // Assert: verificar el estado HTTP, la respuesta JSON y la interacción con el servicio.
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions[0].category").value("Food"));
        verify(transactionService).getTransactions(startDate, endDate, 2L, "Food");
    }

    @Test
    @DisplayName("Debe convertir fechas a LocalDate correctamente")
    void shouldConvertDatesToLocalDate() throws Exception {
        // Arrange: preparar los datos y configurar el comportamiento del servicio simulado.
        LocalDate startDate = LocalDate.of(2024, 7, 10);
        LocalDate endDate = LocalDate.of(2024, 7, 12);
        Map<String, List<TransactionResponse>> response = Map.of("transactions", List.of(
                transactionResponse(1L, "25.00", startDate, "Food", "Lunch", "EXPENSE")));
        when(transactionService.getTransactions(startDate, endDate, null, null)).thenReturn(response);

        // Act: ejecutar la solicitud HTTP mediante MockMvc.
        ResultActions result = mockMvc.perform(get(BASE_PATH + "/transactions")
                .contextPath(BASE_PATH)
                .param("startDate", "2024-07-10")
                .param("endDate", "2024-07-12"));

        // Assert: verificar el estado HTTP, la respuesta JSON y la interacción con el servicio.
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions[0].date").value("2024-07-10"));
        verify(transactionService).getTransactions(startDate, endDate, null, null);
    }

    @Test
    @DisplayName("Debe actualizar una transacción existente con 200 OK")
    void shouldUpdateTransactionSuccessfully() throws Exception {
        // Arrange: preparar los datos y configurar el comportamiento del servicio simulado.
        TransactionUpdateRequest request = TransactionUpdateRequest.builder()
                .amount(new BigDecimal("200.00"))
                .category("Food")
                .description("Updated groceries")
                .build();
        TransactionResponse response = transactionResponse(
                1L, "200.00", LocalDate.of(2024, 9, 10), "Food", "Updated groceries", "EXPENSE");
        when(transactionService.updateTransaction(eq(1L), any(TransactionUpdateRequest.class)))
                .thenReturn(response);

        // Act: ejecutar la solicitud HTTP mediante MockMvc.
        ResultActions result = mockMvc.perform(put(BASE_PATH + "/transactions/1")
                .contextPath(BASE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // Assert: verificar el estado HTTP, la respuesta JSON y la interacción con el servicio.
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(200.0))
                .andExpect(jsonPath("$.category").value("Food"))
                .andExpect(jsonPath("$.description").value("Updated groceries"));

        ArgumentCaptor<TransactionUpdateRequest> captor = ArgumentCaptor.forClass(TransactionUpdateRequest.class);
        verify(transactionService).updateTransaction(eq(1L), captor.capture());
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo("200.00");
        assertThat(captor.getValue().getCategory()).isEqualTo("Food");
        assertThat(captor.getValue().getDescription()).isEqualTo("Updated groceries");
    }

    @Test
    @DisplayName("Debe eliminar una transacción existente con 200 OK")
    void shouldDeleteTransactionSuccessfully() throws Exception {
        // Arrange: preparar los datos y configurar el comportamiento del servicio simulado.
        when(transactionService.deleteTransaction(1L))
                .thenReturn(Map.of("message", "Transaction deleted successfully"));

        // Act: ejecutar la solicitud HTTP mediante MockMvc.
        ResultActions result = mockMvc.perform(delete(BASE_PATH + "/transactions/1")
                .contextPath(BASE_PATH));

        // Assert: verificar el estado HTTP, la respuesta JSON y la interacción con el servicio.
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Transaction deleted successfully"));
        verify(transactionService).deleteTransaction(1L);
    }

    @Test
    @DisplayName("Debe manejar una transacción inexistente con 404 Not Found")
    void shouldHandleMissingTransaction() throws Exception {
        // Arrange: preparar los datos y configurar el comportamiento del servicio simulado.
        doThrow(new ResourceNotFoundException("Transaction not found"))
                .when(transactionService).deleteTransaction(99L);

        // Act: ejecutar la solicitud HTTP mediante MockMvc.
        ResultActions result = mockMvc.perform(delete(BASE_PATH + "/transactions/99")
                .contextPath(BASE_PATH));

        // Assert: verificar el estado HTTP, la respuesta JSON y la interacción con el servicio.
        result.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Transaction not found"));
        verify(transactionService).deleteTransaction(99L);
    }

    @Test
    @DisplayName("Debe rechazar fechas con formato inválido en filtros")
    void shouldRejectMalformedDateParameters() throws Exception {
        // Arrange: preparar una fecha que no puede convertirse a LocalDate.
        String invalidDate = "bad-date";

        // Act: ejecutar la solicitud HTTP mediante MockMvc.
        ResultActions result = mockMvc.perform(get(BASE_PATH + "/transactions")
                .contextPath(BASE_PATH)
                .param("startDate", invalidDate));

        // Assert: verificar el estado HTTP, la respuesta JSON y la interacción con el servicio.
        result.andExpect(status().isBadRequest());
        verifyNoInteractions(transactionService);
    }

    private TransactionResponse transactionResponse(
            Long id,
            String amount,
            LocalDate date,
            String category,
            String description,
            String type) {
        return TransactionResponse.builder()
                .id(id)
                .amount(new BigDecimal(amount))
                .date(date)
                .category(category)
                .description(description)
                .type(type)
                .build();
    }
}
