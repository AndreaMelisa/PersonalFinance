package com.aaryav.finance.service;

import com.aaryav.finance.dto.request.GoalRequest;
import com.aaryav.finance.dto.request.GoalUpdateRequest;
import com.aaryav.finance.dto.response.GoalResponse;
import com.aaryav.finance.entity.Category;
import com.aaryav.finance.entity.CategoryType;
import com.aaryav.finance.entity.Goal;
import com.aaryav.finance.entity.Transaction;
import com.aaryav.finance.entity.User;
import com.aaryav.finance.exception.BadRequestException;
import com.aaryav.finance.exception.ForbiddenException;
import com.aaryav.finance.exception.ResourceNotFoundException;
import com.aaryav.finance.repository.GoalRepository;
import com.aaryav.finance.repository.TransactionRepository;
import com.aaryav.finance.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoalServiceTest {

    @Mock
    private GoalRepository goalRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private GoalService goalService;

    private User currentUser;

    @BeforeEach
    void setUp() {

        currentUser = User.builder().id(1L).username("test@example.com").build();

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("test@example.com", null));

        when(userRepository.findByUsername("test@example.com")).thenReturn(Optional.of(currentUser));
    }

    @Test
    @DisplayName("Debe crear una meta correctamente")
    void createGoal_ShouldCreateSuccessfully() {

        GoalRequest request = new GoalRequest();
        request.setGoalName("Comprar laptop");
        request.setTargetAmount(new BigDecimal("3000.00"));
        request.setTargetDate(LocalDate.now().plusDays(30));
        request.setStartDate(LocalDate.now());

        Goal savedGoal = Goal.builder().id(1L).goalName("Comprar laptop").targetAmount(new BigDecimal("3000.00")).targetDate(request.getTargetDate()).startDate(request.getStartDate()).user(currentUser).build();

        when(goalRepository.save(any(Goal.class))).thenReturn(savedGoal);
        when(transactionRepository.findByUserIdAndDateOnOrAfter(eq(1L), eq(request.getStartDate()))).thenReturn(List.of());

        GoalResponse result = goalService.createGoal(request);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Comprar laptop", result.getGoalName());
        assertEquals(new BigDecimal("3000.00"), result.getTargetAmount());
        assertEquals(0.0, result.getProgressPercentage());
        assertEquals(new BigDecimal("3000.00"), result.getRemainingAmount());

        verify(goalRepository).save(any(Goal.class));
    }

    @Test
    @DisplayName("Debe usar la fecha actual como fecha de inicio")
    void createGoal_ShouldUseCurrentDate() {

        GoalRequest request = new GoalRequest();
        request.setGoalName("Viaje");
        request.setTargetAmount(new BigDecimal("5000"));
        request.setTargetDate(LocalDate.now().plusDays(60));
        request.setStartDate(null);

        Goal savedGoal = Goal.builder().id(2L).goalName("Viaje").targetAmount(new BigDecimal("5000")).targetDate(request.getTargetDate()).startDate(LocalDate.now()).user(currentUser).build();

        when(goalRepository.save(any(Goal.class))).thenReturn(savedGoal);
        when(transactionRepository.findByUserIdAndDateOnOrAfter(eq(1L), eq(LocalDate.now()))).thenReturn(List.of());

        GoalResponse result = goalService.createGoal(request);

        assertNotNull(result);
        assertEquals(LocalDate.now(), result.getStartDate());

        verify(goalRepository).save(any(Goal.class));
    }

    @Test
    @DisplayName("Debe rechazar una fecha objetivo igual a hoy")
    void createGoal_ShouldRejectTodayTarget() {

        GoalRequest request = new GoalRequest();
        request.setGoalName("Meta inválida");
        request.setTargetAmount(new BigDecimal("1000"));
        request.setTargetDate(LocalDate.now());

        assertThrows(BadRequestException.class, () -> goalService.createGoal(request));

        verify(goalRepository, never()).save(any(Goal.class));
    }

    @Test
    @DisplayName("Debe rechazar una fecha objetivo pasada")
    void createGoal_ShouldRejectPastTarget() {

        GoalRequest request = new GoalRequest();
        request.setGoalName("Meta inválida");
        request.setTargetAmount(new BigDecimal("1000"));
        request.setTargetDate(LocalDate.now().minusDays(1));

        assertThrows(BadRequestException.class, () -> goalService.createGoal(request));

        verify(goalRepository, never()).save(any(Goal.class));
    }

    @Test
    @DisplayName("Debe devolver las metas del usuario")
    void getAllGoals_ShouldReturnUserGoals() {

        Goal goal = Goal.builder().id(1L).goalName("Laptop").targetAmount(new BigDecimal("3000")).targetDate(LocalDate.now().plusDays(30)).startDate(LocalDate.now()).user(currentUser).build();

        when(goalRepository.findByUserId(1L)).thenReturn(List.of(goal));
        when(transactionRepository.findByUserIdAndDateOnOrAfter(eq(1L), eq(goal.getStartDate()))).thenReturn(List.of());

        Map<String, List<GoalResponse>> result = goalService.getAllGoals();

        assertNotNull(result);
        assertTrue(result.containsKey("goals"));
        assertEquals(1, result.get("goals").size());
        assertEquals("Laptop", result.get("goals").get(0).getGoalName());

        verify(goalRepository).findByUserId(1L);
    }

    @Test
    @DisplayName("Debe devolver una lista vacía cuando no hay metas")
    void getAllGoals_ShouldReturnEmpty() {

        when(goalRepository.findByUserId(1L)).thenReturn(List.of());

        Map<String, List<GoalResponse>> result = goalService.getAllGoals();

        assertNotNull(result);
        assertTrue(result.containsKey("goals"));
        assertTrue(result.get("goals").isEmpty());

        verify(goalRepository).findByUserId(1L);
    }

    @Test
    @DisplayName("Debe rechazar la consulta cuando el usuario no existe")
    void getAllGoals_ShouldRejectMissingUser() {

        when(userRepository.findByUsername("test@example.com")).thenReturn(Optional.empty());

        Throwable exception = catchThrowable(() -> goalService.getAllGoals());

        assertThat(exception).isInstanceOf(ResourceNotFoundException.class).hasMessageContaining("User not found");

        verify(goalRepository, never()).findByUserId(anyLong());
    }

    @Test
    @DisplayName("Debe devolver la meta del usuario")
    void getGoalById_ShouldReturnUserGoal() {

        Goal goal = Goal.builder().id(1L).goalName("Laptop").targetAmount(new BigDecimal("3000")).targetDate(LocalDate.now().plusDays(30)).startDate(LocalDate.now()).user(currentUser).build();

        when(goalRepository.findById(1L)).thenReturn(Optional.of(goal));
        when(transactionRepository.findByUserIdAndDateOnOrAfter(eq(1L), eq(goal.getStartDate()))).thenReturn(List.of());

        GoalResponse result = goalService.getGoalById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Laptop", result.getGoalName());

        verify(goalRepository).findById(1L);
    }

    @Test
    @DisplayName("Debe calcular correctamente el progreso de la meta")
    void getGoalById_ShouldCalculateProgress() {

        Goal goal = Goal.builder().id(1L).goalName("Laptop").targetAmount(new BigDecimal("3000")).targetDate(LocalDate.now().plusDays(30)).startDate(LocalDate.now()).user(currentUser).build();

        Category incomeCategory = new Category();
        incomeCategory.setType(CategoryType.INCOME);

        Category expenseCategory = new Category();
        expenseCategory.setType(CategoryType.EXPENSE);

        Transaction income = new Transaction();
        income.setAmount(new BigDecimal("2000"));
        income.setCategory(incomeCategory);

        Transaction expense = new Transaction();
        expense.setAmount(new BigDecimal("500"));
        expense.setCategory(expenseCategory);

        when(goalRepository.findById(1L)).thenReturn(Optional.of(goal));
        when(transactionRepository.findByUserIdAndDateOnOrAfter(eq(1L), eq(goal.getStartDate()))).thenReturn(List.of(income, expense));

        GoalResponse result = goalService.getGoalById(1L);

        assertEquals(new BigDecimal("1500"), result.getCurrentProgress());
        assertEquals(50.0, result.getProgressPercentage());
        assertEquals(new BigDecimal("1500"), result.getRemainingAmount());
    }

    @Test
    @DisplayName("Debe establecer el monto restante en cero cuando se supera la meta")
    void getGoalById_ShouldSetZeroRemainingWhenExceeded() {

        Goal goal = Goal.builder().id(1L).goalName("Laptop").targetAmount(new BigDecimal("3000")).targetDate(LocalDate.now().plusDays(30)).startDate(LocalDate.now()).user(currentUser).build();

        Category incomeCategory = new Category();
        incomeCategory.setType(CategoryType.INCOME);

        Transaction income = new Transaction();
        income.setAmount(new BigDecimal("4000"));
        income.setCategory(incomeCategory);

        when(goalRepository.findById(1L)).thenReturn(Optional.of(goal));
        when(transactionRepository.findByUserIdAndDateOnOrAfter(eq(1L), eq(goal.getStartDate()))).thenReturn(List.of(income));

        GoalResponse result = goalService.getGoalById(1L);

        assertEquals(new BigDecimal("4000"), result.getCurrentProgress());
        assertEquals(133.33, result.getProgressPercentage());
        assertEquals(BigDecimal.ZERO, result.getRemainingAmount());
    }

    @Test
    @DisplayName("Debe establecer el progreso en cero cuando el monto objetivo es cero")
    void getGoalById_ShouldReturnZeroProgress() {

        Goal goal = Goal.builder().id(1L).goalName("Meta sin monto").targetAmount(BigDecimal.ZERO).targetDate(LocalDate.now().plusDays(30)).startDate(LocalDate.now()).user(currentUser).build();

        when(goalRepository.findById(1L)).thenReturn(Optional.of(goal));
        when(transactionRepository.findByUserIdAndDateOnOrAfter(eq(1L), eq(goal.getStartDate()))).thenReturn(List.of());

        GoalResponse result = goalService.getGoalById(1L);

        assertNotNull(result);
        assertEquals(0.0, result.getProgressPercentage());
        assertEquals(BigDecimal.ZERO, result.getRemainingAmount());
    }

    @Test
    @DisplayName("Debe rechazar una meta que no existe")
    void getGoalById_ShouldRejectMissingGoal() {

        when(goalRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> goalService.getGoalById(99L));
    }

    @Test
    @DisplayName("Debe rechazar el acceso a una meta de otro usuario")
    void getGoalById_ShouldRejectOtherUser() {

        User anotherUser = User.builder().id(2L).username("otheruser").build();

        Goal goal = Goal.builder().id(1L).goalName("Meta de otro usuario").targetAmount(new BigDecimal("2000")).targetDate(LocalDate.now().plusDays(30)).startDate(LocalDate.now()).user(anotherUser).build();

        when(goalRepository.findById(1L)).thenReturn(Optional.of(goal));

        assertThrows(ForbiddenException.class, () -> goalService.getGoalById(1L));

        verify(transactionRepository, never()).findByUserIdAndDateOnOrAfter(anyLong(), any());
    }

    @Test
    @DisplayName("Debe actualizar el monto de la meta")
    void updateGoal_ShouldUpdateAmount() {

        Goal goal = Goal.builder().id(1L).goalName("Laptop").targetAmount(new BigDecimal("3000")).targetDate(LocalDate.now().plusDays(30)).startDate(LocalDate.now()).user(currentUser).build();

        GoalUpdateRequest request = new GoalUpdateRequest();
        request.setTargetAmount(new BigDecimal("4000"));

        when(goalRepository.findById(1L)).thenReturn(Optional.of(goal));
        when(goalRepository.save(goal)).thenReturn(goal);
        when(transactionRepository.findByUserIdAndDateOnOrAfter(eq(1L), eq(goal.getStartDate()))).thenReturn(List.of());

        GoalResponse result = goalService.updateGoal(1L, request);

        assertNotNull(result);
        assertEquals(new BigDecimal("4000"), result.getTargetAmount());

        verify(goalRepository).save(goal);
    }

    @Test
    @DisplayName("Debe actualizar el monto y la fecha de la meta")
    void updateGoal_ShouldUpdateAmountAndDate() {

        Goal goal = Goal.builder().id(1L).goalName("Laptop").targetAmount(new BigDecimal("3000")).targetDate(LocalDate.now().plusDays(30)).startDate(LocalDate.now()).user(currentUser).build();

        LocalDate newTargetDate = LocalDate.now().plusDays(90);

        GoalUpdateRequest request = new GoalUpdateRequest();
        request.setTargetAmount(new BigDecimal("5000"));
        request.setTargetDate(newTargetDate);

        when(goalRepository.findById(1L)).thenReturn(Optional.of(goal));
        when(goalRepository.save(goal)).thenReturn(goal);
        when(transactionRepository.findByUserIdAndDateOnOrAfter(eq(1L), eq(goal.getStartDate()))).thenReturn(List.of());

        GoalResponse result = goalService.updateGoal(1L, request);

        assertNotNull(result);
        assertEquals(new BigDecimal("5000"), result.getTargetAmount());
        assertEquals(newTargetDate, result.getTargetDate());

        verify(goalRepository).save(goal);
    }

    @Test
    @DisplayName("Debe actualizar la fecha de la meta")
    void updateGoal_ShouldUpdateDate() {

        Goal goal = Goal.builder().id(1L).goalName("Laptop").targetAmount(new BigDecimal("3000")).targetDate(LocalDate.now().plusDays(30)).startDate(LocalDate.now()).user(currentUser).build();
        LocalDate newDate = LocalDate.now().plusDays(60);
        GoalUpdateRequest request = new GoalUpdateRequest();
        request.setTargetDate(newDate);

        when(goalRepository.findById(1L)).thenReturn(Optional.of(goal));
        when(goalRepository.save(goal)).thenReturn(goal);
        when(transactionRepository.findByUserIdAndDateOnOrAfter(eq(1L), eq(goal.getStartDate()))).thenReturn(List.of());

        GoalResponse result = goalService.updateGoal(1L, request);

        assertNotNull(result);
        assertEquals(newDate, result.getTargetDate());

        verify(goalRepository).save(goal);
    }

    @Test
    @DisplayName("Debe conservar los valores existentes cuando no se envían cambios")
    void updateGoal_ShouldKeepExistingValues() {

        BigDecimal originalAmount = new BigDecimal("3000");
        LocalDate originalDate = LocalDate.now().plusDays(30);

        Goal goal = Goal.builder().id(1L).goalName("Laptop").targetAmount(originalAmount).targetDate(originalDate).startDate(LocalDate.now()).user(currentUser).build();
        GoalUpdateRequest request = new GoalUpdateRequest();

        when(goalRepository.findById(1L)).thenReturn(Optional.of(goal));
        when(goalRepository.save(goal)).thenReturn(goal);
        when(transactionRepository.findByUserIdAndDateOnOrAfter(eq(1L), eq(goal.getStartDate()))).thenReturn(List.of());

        GoalResponse result = goalService.updateGoal(1L, request);

        assertNotNull(result);
        assertEquals(originalAmount, result.getTargetAmount());
        assertEquals(originalDate, result.getTargetDate());

        verify(goalRepository).save(goal);
    }

    @Test
    @DisplayName("Debe rechazar una fecha objetivo inválida")
    void updateGoal_ShouldRejectInvalidDate() {

        Goal goal = Goal.builder().id(1L).goalName("Laptop").targetAmount(new BigDecimal("3000")).targetDate(LocalDate.now().plusDays(30)).startDate(LocalDate.now()).user(currentUser).build();
        GoalUpdateRequest request = new GoalUpdateRequest();
        request.setTargetDate(LocalDate.now());

        when(goalRepository.findById(1L)).thenReturn(Optional.of(goal));

        assertThrows(BadRequestException.class, () -> goalService.updateGoal(1L, request));

        verify(goalRepository, never()).save(any(Goal.class));
    }

    @Test
    @DisplayName("Debe rechazar la actualización de una meta de otro usuario")
    void updateGoal_ShouldRejectOtherUser() {

        User anotherUser = User.builder().id(2L).username("otheruser").build();

        Goal goal = Goal.builder().id(1L).goalName("Otra meta").targetAmount(new BigDecimal("2000")).targetDate(LocalDate.now().plusDays(30)).startDate(LocalDate.now()).user(anotherUser).build();
        GoalUpdateRequest request = new GoalUpdateRequest();
        request.setTargetAmount(new BigDecimal("5000"));

        when(goalRepository.findById(1L)).thenReturn(Optional.of(goal));

        assertThrows(ForbiddenException.class, () -> goalService.updateGoal(1L, request));

        verify(goalRepository, never()).save(any(Goal.class));
    }

    @Test
    @DisplayName("Debe rechazar la actualización de una meta que no existe")
    void updateGoal_ShouldRejectMissingGoal() {

        when(goalRepository.findById(99L)).thenReturn(Optional.empty());

        GoalUpdateRequest request = new GoalUpdateRequest();

        Throwable exception = catchThrowable(() -> goalService.updateGoal(99L, request));

        assertThat(exception).isInstanceOf(ResourceNotFoundException.class).hasMessageContaining("Goal not found");

        verify(goalRepository, never()).save(any(Goal.class));
    }

    @Test
    @DisplayName("Debe eliminar la meta correctamente")
    void deleteGoal_ShouldDeleteSuccessfully() {

        Goal goal = Goal.builder().id(1L).goalName("Laptop").targetAmount(new BigDecimal("3000")).targetDate(LocalDate.now().plusDays(30)).startDate(LocalDate.now()).user(currentUser).build();

        when(goalRepository.findById(1L)).thenReturn(Optional.of(goal));

        Map<String, String> result = goalService.deleteGoal(1L);

        assertNotNull(result);
        assertEquals("Goal deleted successfully", result.get("message"));

        verify(goalRepository).delete(goal);
    }

    @Test
    @DisplayName("Debe rechazar la eliminación de una meta que no existe")
    void deleteGoal_ShouldRejectMissingGoal() {

        when(goalRepository.findById(99L)).thenReturn(Optional.empty());

        Throwable exception = catchThrowable(() -> goalService.deleteGoal(99L));

        assertThat(exception).isInstanceOf(ResourceNotFoundException.class).hasMessageContaining("Goal not found");

        verify(goalRepository, never()).delete(any(Goal.class));
    }

    @Test
    @DisplayName("Debe rechazar la eliminación de una meta de otro usuario")
    void deleteGoal_ShouldRejectOtherUser() {

        User anotherUser = User.builder().id(2L).username("otheruser").build();

        Goal goal = Goal.builder().id(1L).goalName("Meta ajena").targetAmount(new BigDecimal("2000")).targetDate(LocalDate.now().plusDays(30)).startDate(LocalDate.now()).user(anotherUser).build();

        when(goalRepository.findById(1L)).thenReturn(Optional.of(goal));

        assertThrows(ForbiddenException.class, () -> goalService.deleteGoal(1L));

        verify(goalRepository, never()).delete(any(Goal.class));
    }
}
