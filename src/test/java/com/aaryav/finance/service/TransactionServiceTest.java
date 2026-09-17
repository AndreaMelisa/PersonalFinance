package com.aaryav.finance.service;

import com.aaryav.finance.dto.request.TransactionRequest;
import com.aaryav.finance.dto.request.TransactionUpdateRequest;
import com.aaryav.finance.dto.response.TransactionResponse;
import com.aaryav.finance.entity.Category;
import com.aaryav.finance.entity.CategoryType;
import com.aaryav.finance.entity.Transaction;
import com.aaryav.finance.entity.User;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TransactionService transactionService;

    private User testUser;
    private Category salaryCategory;

    @BeforeEach
    void setUp() {

        testUser = User.builder().id(1L).username("test@example.com").build();
        salaryCategory = Category.builder().id(1L).name("Salary").type(CategoryType.INCOME).custom(false).build();

        configureAuthenticatedUser("test@example.com");

        lenient().when(userRepository.findByUsername("test@example.com")).thenReturn(Optional.of(testUser));
    }

    @Test
    @DisplayName("Debe crear una transacción correctamente")
    void createTransaction_ShouldSucceed() {

        TransactionRequest request = TransactionRequest.builder().amount(new BigDecimal("50000.00")).date(LocalDate.of(2024, 1, 15)).category("Salary").description("January Salary").build();

        when(categoryRepository.findByNameAccessibleByUser("Salary", 1L)).thenReturn(Optional.of(salaryCategory));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction transaction = invocation.getArgument(0);
            transaction.setId(1L);
            return transaction;
        });

        TransactionResponse result = transactionService.createTransaction(request);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getAmount()).isEqualByComparingTo("50000.00");
        assertThat(result.getCategory()).isEqualTo("Salary");
        assertThat(result.getType()).isEqualTo("INCOME");
        assertThat(result.getDate()).isEqualTo(LocalDate.of(2024, 1, 15));
        assertThat(result.getDescription()).isEqualTo("January Salary");

        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Debe rechazar una transacción con fecha futura")
    void createTransaction_ShouldRejectFutureDate() {

        TransactionRequest request = TransactionRequest.builder().amount(new BigDecimal("100")).date(LocalDate.now().plusDays(1)).category("Salary").build();
        assertThatThrownBy(() -> transactionService.createTransaction(request))

                .isInstanceOf(BadRequestException.class).hasMessageContaining("future");

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Debe rechazar una categoría inexistente al crear")
    void createTransaction_ShouldRejectInvalidCategory() {

        TransactionRequest request = TransactionRequest.builder().amount(new BigDecimal("100")).date(LocalDate.of(2024, 1, 1)).category("CategoriaInexistente").build();

        when(categoryRepository.findByNameAccessibleByUser("CategoriaInexistente", 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.createTransaction(request))

                .isInstanceOf(BadRequestException.class).hasMessageContaining("Invalid category");

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Debe obtener transacciones utilizando filtros")
    void getTransactions_ShouldApplyFilters() {

        Transaction transaction = Transaction.builder().id(1L).amount(new BigDecimal("50000")).date(LocalDate.of(2024, 1, 15)).category(salaryCategory).user(testUser).description("Salario").build();
        when(transactionRepository.findByFilters(1L, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31), null)).thenReturn(List.of(transaction));

        Map<String, List<TransactionResponse>> result = transactionService.getTransactions(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31), null, null);

        assertThat(result).containsKey("transactions");
        assertThat(result.get("transactions")).hasSize(1);
        assertThat(result.get("transactions").get(0).getAmount()).isEqualByComparingTo("50000");
        assertThat(result.get("transactions").get(0).getCategory()).isEqualTo("Salary");
    }

    @Test
    @DisplayName("Debe obtener una lista vacía cuando no existen transacciones")
    void getTransactions_ShouldReturnEmpty() {

        when(transactionRepository.findByFilters(1L, null, null, null)).thenReturn(List.of());

        Map<String, List<TransactionResponse>> result = transactionService.getTransactions(null, null, null, null);

        assertThat(result).containsKey("transactions");
        assertThat(result.get("transactions")).isEmpty();
    }

    @Test
    @DisplayName("Debe resolver el ID de categoría cuando se proporciona el nombre")
    void getTransactions_ShouldResolveCategoryName() {

        when(categoryRepository.findByNameAccessibleByUser("Salary", 1L)).thenReturn(Optional.of(salaryCategory));
        when(transactionRepository.findByFilters(1L, null, null, 1L)).thenReturn(List.of());

        Map<String, List<TransactionResponse>> result = transactionService.getTransactions(null, null, null, "Salary");

        assertThat(result.get("transactions")).isEmpty();

        verify(categoryRepository).findByNameAccessibleByUser("Salary", 1L);
        verify(transactionRepository).findByFilters(1L, null, null, 1L);
    }

    @Test
    @DisplayName("No debe buscar categoría cuando el nombre está vacío")
    void getTransactions_ShouldIgnoreEmptyCategory() {

        when(transactionRepository.findByFilters(1L, null, null, null)).thenReturn(List.of());

        Map<String, List<TransactionResponse>> result = transactionService.getTransactions(null, null, null, "");

        assertThat(result.get("transactions")).isEmpty();

        verify(categoryRepository, never()).findByNameAccessibleByUser(anyString(), anyLong());
        verify(transactionRepository).findByFilters(1L, null, null, null);
    }

    @Test
    @DisplayName("No debe buscar categoría cuando el nombre contiene solo espacios")
    void getTransactions_ShouldIgnoreBlankCategory() {

        when(transactionRepository.findByFilters(1L, null, null, null)).thenReturn(List.of());

        Map<String, List<TransactionResponse>> result = transactionService.getTransactions(null, null, null, "   ");

        assertThat(result.get("transactions")).isEmpty();

        verify(categoryRepository, never()).findByNameAccessibleByUser(anyString(), anyLong());
        verify(transactionRepository).findByFilters(1L, null, null, null);
    }

    @Test
    @DisplayName("Debe utilizar directamente el ID cuando se proporciona categoría")
    void getTransactions_ShouldUseCategoryId() {

        when(transactionRepository.findByFilters(1L, null, null, 1L)).thenReturn(List.of());

        Map<String, List<TransactionResponse>> result = transactionService.getTransactions(null, null, 1L, "Salary");

        assertThat(result.get("transactions")).isEmpty();

        verify(categoryRepository, never()).findByNameAccessibleByUser(anyString(), anyLong());
        verify(transactionRepository).findByFilters(1L, null, null, 1L);
    }

    @Test
    @DisplayName("Debe continuar sin categoría cuando el nombre no existe")
    void getTransactions_ShouldIgnoreUnknownCategory() {

        when(categoryRepository.findByNameAccessibleByUser("NoExiste", 1L)).thenReturn(Optional.empty());
        when(transactionRepository.findByFilters(1L, null, null, null)).thenReturn(List.of());

        Map<String, List<TransactionResponse>> result = transactionService.getTransactions(null, null, null, "NoExiste");

        assertThat(result.get("transactions")).isEmpty();

        verify(categoryRepository).findByNameAccessibleByUser("NoExiste", 1L);
        verify(transactionRepository).findByFilters(1L, null, null, null);
    }

    @Test
    @DisplayName("Debe rechazar la operación cuando el usuario no existe")
    void getTransactions_ShouldRejectMissingUser() {

        when(userRepository.findByUsername("test@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.getTransactions(null, null, null, null))

                .isInstanceOf(ResourceNotFoundException.class).hasMessageContaining("User not found");

        verify(transactionRepository, never()).findByFilters(anyLong(), any(), any(), any());
    }

    @Test
    @DisplayName("Debe actualizar correctamente el monto")
    void updateTransaction_ShouldUpdateAmount() {

        Transaction existingTransaction = Transaction.builder().id(1L).amount(new BigDecimal("50000")).date(LocalDate.of(2024, 1, 15)).category(salaryCategory).user(testUser).description("Anterior").build();

        when(transactionRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(existingTransaction));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionUpdateRequest update = TransactionUpdateRequest.builder().amount(new BigDecimal("60000")).description("Actualizado").build();

        TransactionResponse result = transactionService.updateTransaction(1L, update);

        assertThat(result.getAmount()).isEqualByComparingTo("60000");
        assertThat(result.getDescription()).isEqualTo("Actualizado");

        verify(transactionRepository).save(existingTransaction);
    }

    @Test
    @DisplayName("Debe actualizar correctamente la categoría")
    void updateTransaction_ShouldUpdateCategory() {

        Category newCategory = Category.builder().id(2L).name("Food").type(CategoryType.EXPENSE).custom(false).build();
        Transaction existingTransaction = Transaction.builder().id(1L).amount(new BigDecimal("100")).date(LocalDate.of(2024, 1, 15)).category(salaryCategory).user(testUser).build();

        when(transactionRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(existingTransaction));
        when(categoryRepository.findByNameAccessibleByUser("Food", 1L)).thenReturn(Optional.of(newCategory));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionUpdateRequest update = TransactionUpdateRequest.builder().category("Food").build();

        TransactionResponse result = transactionService.updateTransaction(1L, update);

        assertThat(result.getCategory()).isEqualTo("Food");
        assertThat(result.getType()).isEqualTo("EXPENSE");

        verify(categoryRepository).findByNameAccessibleByUser("Food", 1L);
        verify(transactionRepository).save(existingTransaction);
    }

    @Test
    @DisplayName("Debe actualizar correctamente la descripción")
    void updateTransaction_ShouldUpdateDescription() {

        Transaction existingTransaction = Transaction.builder().id(1L).amount(new BigDecimal("50000")).date(LocalDate.of(2024, 1, 15)).category(salaryCategory).user(testUser).description("Descripción anterior").build();

        when(transactionRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(existingTransaction));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionUpdateRequest update = TransactionUpdateRequest.builder().description("Nueva descripción").build();

        TransactionResponse result = transactionService.updateTransaction(1L, update);

        assertThat(result.getDescription()).isEqualTo("Nueva descripción");

        verify(transactionRepository).save(existingTransaction);
    }

    @Test
    @DisplayName("Debe permitir actualizar sin cambiar el monto")
    void updateTransaction_ShouldKeepAmount() {

        Transaction existingTransaction = Transaction.builder().id(1L).amount(new BigDecimal("50000")).date(LocalDate.of(2024, 1, 15)).category(salaryCategory).user(testUser).build();

        when(transactionRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(existingTransaction));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionUpdateRequest update = TransactionUpdateRequest.builder().description("Solo descripción").build();

        TransactionResponse result = transactionService.updateTransaction(1L, update);

        assertThat(result.getAmount()).isEqualByComparingTo("50000");
        assertThat(result.getDescription()).isEqualTo("Solo descripción");

        verify(transactionRepository).save(existingTransaction);
    }

    @Test
    @DisplayName("Debe permitir actualizar sin cambiar la categoría")
    void updateTransaction_ShouldKeepCategory() {

        Transaction existingTransaction = Transaction.builder().id(1L).amount(new BigDecimal("50000")).date(LocalDate.of(2024, 1, 15)).category(salaryCategory).user(testUser).build();

        when(transactionRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(existingTransaction));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionUpdateRequest update = TransactionUpdateRequest.builder().amount(new BigDecimal("55000")).build();

        TransactionResponse result = transactionService.updateTransaction(1L, update);

        assertThat(result.getAmount()).isEqualByComparingTo("55000");
        assertThat(result.getCategory()).isEqualTo("Salary");

        verify(categoryRepository, never()).findByNameAccessibleByUser(anyString(), anyLong());
        verify(transactionRepository).save(existingTransaction);
    }

    @Test
    @DisplayName("Debe rechazar un monto igual a cero")
    void updateTransaction_ShouldRejectZeroAmount() {
        Transaction existingTransaction = Transaction.builder().id(1L).amount(new BigDecimal("50000")).date(LocalDate.of(2024, 1, 15)).category(salaryCategory).user(testUser).build();

        when(transactionRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(existingTransaction));

        TransactionUpdateRequest update = TransactionUpdateRequest.builder().amount(BigDecimal.ZERO).build();

        assertThatThrownBy(() -> transactionService.updateTransaction(1L, update))

                .isInstanceOf(BadRequestException.class).hasMessageContaining("Amount must be positive");

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Debe rechazar un monto negativo")
    void updateTransaction_ShouldRejectNegativeAmount() {

        Transaction existingTransaction = Transaction.builder().id(1L).amount(new BigDecimal("50000")).date(LocalDate.of(2024, 1, 15)).category(salaryCategory).user(testUser).build();

        when(transactionRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(existingTransaction));

        TransactionUpdateRequest update = TransactionUpdateRequest.builder().amount(new BigDecimal("-100")).build();

        assertThatThrownBy(() -> transactionService.updateTransaction(1L, update))

                .isInstanceOf(BadRequestException.class).hasMessageContaining("Amount must be positive");

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Debe rechazar la actualización de una transacción inexistente")
    void updateTransaction_ShouldRejectMissingTransaction() {

        when(transactionRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        TransactionUpdateRequest update = new TransactionUpdateRequest();

        assertThatThrownBy(() -> transactionService.updateTransaction(99L, update))

                .isInstanceOf(ResourceNotFoundException.class).hasMessageContaining("Transaction not found");

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Debe rechazar una categoría inválida al actualizar")
    void updateTransaction_ShouldRejectInvalidCategory() {

        Transaction existingTransaction = Transaction.builder().id(1L).amount(new BigDecimal("50000")).date(LocalDate.of(2024, 1, 15)).category(salaryCategory).user(testUser).build();

        when(transactionRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(existingTransaction));
        when(categoryRepository.findByNameAccessibleByUser("CategoriaInvalida", 1L)).thenReturn(Optional.empty());

        TransactionUpdateRequest update = TransactionUpdateRequest.builder().category("CategoriaInvalida").build();

        assertThatThrownBy(() -> transactionService.updateTransaction(1L, update))

                .isInstanceOf(BadRequestException.class).hasMessageContaining("Invalid category");

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Debe eliminar correctamente una transacción")
    void deleteTransaction_ShouldSucceed() {

        Transaction existingTransaction = Transaction.builder().id(1L).user(testUser).category(salaryCategory).build();

        when(transactionRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(existingTransaction));

        Map<String, String> result = transactionService.deleteTransaction(1L);

        assertThat(result.get("message")).isEqualTo("Transaction deleted successfully");

        verify(transactionRepository).delete(existingTransaction);
    }

    @Test
    @DisplayName("Debe rechazar la eliminación de una transacción inexistente")
    void deleteTransaction_ShouldRejectMissingTransaction() {

        when(transactionRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.deleteTransaction(99L))

                .isInstanceOf(ResourceNotFoundException.class).hasMessageContaining("Transaction not found");

        verify(transactionRepository, never()).delete(any(Transaction.class));
    }

    private void configureAuthenticatedUser(String username) {

        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);

        lenient().when(authentication.getName()).thenReturn(username);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);

        SecurityContextHolder.setContext(securityContext);
    }
}
