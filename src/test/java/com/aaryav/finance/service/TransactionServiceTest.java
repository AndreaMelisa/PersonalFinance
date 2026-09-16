package com.aaryav.finance.service;

import com.aaryav.finance.dto.request.TransactionRequest;
import com.aaryav.finance.dto.request.TransactionUpdateRequest;
import com.aaryav.finance.dto.response.TransactionResponse;
import com.aaryav.finance.entity.*;
import com.aaryav.finance.exception.BadRequestException;
import com.aaryav.finance.exception.ResourceNotFoundException;
import com.aaryav.finance.repository.CategoryRepository;
import com.aaryav.finance.repository.TransactionRepository;
import com.aaryav.finance.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private UserRepository userRepository;
    @InjectMocks private TransactionService transactionService;

    private User testUser;
    private Category salaryCategory;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).username("test@example.com").build();
        salaryCategory = Category.builder().id(1L).name("Salary").type(CategoryType.INCOME).custom(false).build();
        mockSecurityContext("test@example.com");
        lenient().when(userRepository.findByUsername("test@example.com")).thenReturn(Optional.of(testUser));
    }

    @Test
    @DisplayName("Should create transaction successfully")
    void createTransaction_Success() {
        TransactionRequest request = TransactionRequest.builder()
                .amount(new BigDecimal("50000.00"))
                .date(LocalDate.of(2024, 1, 15))
                .category("Salary")
                .description("January Salary")
                .build();
        when(categoryRepository.findByNameAccessibleByUser("Salary", 1L)).thenReturn(Optional.of(salaryCategory));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });

        TransactionResponse result = transactionService.createTransaction(request);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getAmount()).isEqualByComparingTo("50000.00");
        assertThat(result.getCategory()).isEqualTo("Salary");
        assertThat(result.getType()).isEqualTo("INCOME");
    }

    @Test
    @DisplayName("Should reject future date transaction")
    void createTransaction_FutureDate() {
        TransactionRequest request = TransactionRequest.builder()
                .amount(new BigDecimal("100"))
                .date(LocalDate.now().plusDays(1))
                .category("Salary")
                .build();

        assertThatThrownBy(() -> transactionService.createTransaction(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("future");
    }

    @Test
    @DisplayName("Should reject invalid category")
    void createTransaction_InvalidCategory() {
        TransactionRequest request = TransactionRequest.builder()
                .amount(new BigDecimal("100"))
                .date(LocalDate.of(2024, 1, 1))
                .category("NonExistent")
                .build();
        when(categoryRepository.findByNameAccessibleByUser("NonExistent", 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.createTransaction(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid category");
    }

    @Test
    @DisplayName("Should get transactions with filters")
    void getTransactions_WithFilters() {
        Transaction t = Transaction.builder().id(1L).amount(new BigDecimal("50000"))
                .date(LocalDate.of(2024, 1, 15)).category(salaryCategory).user(testUser).build();
        when(transactionRepository.findByFilters(1L, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31), null))
                .thenReturn(List.of(t));

        Map<String, List<TransactionResponse>> result = transactionService.getTransactions(
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31), null, null);

        assertThat(result.get("transactions")).hasSize(1);
    }

    @Test
    @DisplayName("Should get transactions without filters")
    void getTransactions_NoFilters() {
        when(transactionRepository.findByFilters(1L, null, null, null)).thenReturn(List.of());

        Map<String, List<TransactionResponse>> result = transactionService.getTransactions(null, null, null, null);

        assertThat(result.get("transactions")).isEmpty();
    }

    @Test
    @DisplayName("Should update transaction amount")
    void updateTransaction_Amount() {
        Transaction existing = Transaction.builder().id(1L).amount(new BigDecimal("50000"))
                .date(LocalDate.of(2024, 1, 15)).category(salaryCategory).user(testUser).build();
        when(transactionRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(existing));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        TransactionUpdateRequest update = TransactionUpdateRequest.builder()
                .amount(new BigDecimal("60000")).description("Updated").build();

        TransactionResponse result = transactionService.updateTransaction(1L, update);

        assertThat(result.getAmount()).isEqualByComparingTo("60000");
        assertThat(result.getDescription()).isEqualTo("Updated");
    }

    @Test
    @DisplayName("Should throw 404 for updating non-existent transaction")
    void updateTransaction_NotFound() {
        when(transactionRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.updateTransaction(99L, new TransactionUpdateRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should delete transaction successfully")
    void deleteTransaction_Success() {
        Transaction existing = Transaction.builder().id(1L).user(testUser).category(salaryCategory).build();
        when(transactionRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(existing));

        Map<String, String> result = transactionService.deleteTransaction(1L);

        assertThat(result.get("message")).isEqualTo("Transaction deleted successfully");
        verify(transactionRepository).delete(existing);
    }

    @Test
    @DisplayName("Should throw 404 for deleting non-existent transaction")
    void deleteTransaction_NotFound() {
        when(transactionRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.deleteTransaction(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private void mockSecurityContext(String username) {
        SecurityContext context = mock(SecurityContext.class);
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getName()).thenReturn(username);
        lenient().when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);
    }
}
