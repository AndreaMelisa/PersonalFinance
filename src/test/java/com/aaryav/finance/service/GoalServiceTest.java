package com.aaryav.finance.service;

import com.aaryav.finance.dto.request.GoalRequest;
import com.aaryav.finance.dto.request.GoalUpdateRequest;
import com.aaryav.finance.dto.response.GoalResponse;
import com.aaryav.finance.entity.*;
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
class GoalServiceTest {

    @Mock private GoalRepository goalRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private UserRepository userRepository;
    @InjectMocks private GoalService goalService;

    private User testUser;
    private User otherUser;
    private Category salaryCategory;
    private Category foodCategory;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).username("test@example.com").build();
        otherUser = User.builder().id(2L).username("other@example.com").build();
        salaryCategory = Category.builder().id(1L).name("Salary").type(CategoryType.INCOME).build();
        foodCategory = Category.builder().id(2L).name("Food").type(CategoryType.EXPENSE).build();
        mockSecurityContext("test@example.com");
        lenient().when(userRepository.findByUsername("test@example.com")).thenReturn(Optional.of(testUser));
    }

    @Test
    @DisplayName("Should create goal with default startDate")
    void createGoal_DefaultStartDate() {
        GoalRequest request = GoalRequest.builder()
                .goalName("Emergency Fund").targetAmount(new BigDecimal("5000"))
                .targetDate(LocalDate.now().plusYears(1)).build();
        when(transactionRepository.findByUserIdAndDateOnOrAfter(eq(1L), any())).thenReturn(List.of());
        when(goalRepository.save(any(Goal.class))).thenAnswer(inv -> {
            Goal g = inv.getArgument(0); g.setId(1L); return g;
        });

        GoalResponse result = goalService.createGoal(request);

        assertThat(result.getGoalName()).isEqualTo("Emergency Fund");
        assertThat(result.getStartDate()).isEqualTo(LocalDate.now());
        assertThat(result.getCurrentProgress()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Should create goal with explicit startDate")
    void createGoal_ExplicitStartDate() {
        GoalRequest request = GoalRequest.builder()
                .goalName("Fund").targetAmount(new BigDecimal("5000"))
                .targetDate(LocalDate.now().plusYears(1)).startDate(LocalDate.of(2025, 1, 1)).build();
        when(transactionRepository.findByUserIdAndDateOnOrAfter(eq(1L), any())).thenReturn(List.of());
        when(goalRepository.save(any(Goal.class))).thenAnswer(inv -> {
            Goal g = inv.getArgument(0); g.setId(1L); return g;
        });

        GoalResponse result = goalService.createGoal(request);
        assertThat(result.getStartDate()).isEqualTo(LocalDate.of(2025, 1, 1));
    }

    @Test
    @DisplayName("Should reject past target date")
    void createGoal_PastTargetDate() {
        GoalRequest request = GoalRequest.builder()
                .goalName("Fund").targetAmount(new BigDecimal("5000"))
                .targetDate(LocalDate.of(2020, 1, 1)).build();

        assertThatThrownBy(() -> goalService.createGoal(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("future");
    }

    @Test
    @DisplayName("Should calculate progress correctly")
    void getGoalById_WithProgress() {
        Goal goal = Goal.builder().id(1L).goalName("Fund").targetAmount(new BigDecimal("5000"))
                .targetDate(LocalDate.now().plusYears(1)).startDate(LocalDate.of(2024, 1, 1)).user(testUser).build();
        Transaction income = Transaction.builder().amount(new BigDecimal("3000")).category(salaryCategory).build();
        Transaction expense = Transaction.builder().amount(new BigDecimal("1000")).category(foodCategory).build();

        when(goalRepository.findById(1L)).thenReturn(Optional.of(goal));
        when(transactionRepository.findByUserIdAndDateOnOrAfter(1L, LocalDate.of(2024, 1, 1)))
                .thenReturn(List.of(income, expense));

        GoalResponse result = goalService.getGoalById(1L);

        assertThat(result.getCurrentProgress()).isEqualByComparingTo("2000");
        assertThat(result.getProgressPercentage()).isEqualTo(40.0);
        assertThat(result.getRemainingAmount()).isEqualByComparingTo("3000");
    }

    @Test
    @DisplayName("Should return all goals for user")
    void getAllGoals_Success() {
        Goal g1 = Goal.builder().id(1L).goalName("Fund1").targetAmount(new BigDecimal("5000"))
                .targetDate(LocalDate.now().plusYears(1)).startDate(LocalDate.now()).user(testUser).build();
        when(goalRepository.findByUserId(1L)).thenReturn(List.of(g1));
        when(transactionRepository.findByUserIdAndDateOnOrAfter(eq(1L), any())).thenReturn(List.of());

        Map<String, List<GoalResponse>> result = goalService.getAllGoals();
        assertThat(result.get("goals")).hasSize(1);
    }

    @Test
    @DisplayName("Should throw 403 for accessing other user's goal")
    void getGoalById_Forbidden() {
        Goal otherGoal = Goal.builder().id(2L).user(otherUser).build();
        when(goalRepository.findById(2L)).thenReturn(Optional.of(otherGoal));

        assertThatThrownBy(() -> goalService.getGoalById(2L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Should throw 404 for non-existent goal")
    void getGoalById_NotFound() {
        when(goalRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> goalService.getGoalById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should update goal target amount")
    void updateGoal_Success() {
        Goal goal = Goal.builder().id(1L).goalName("Fund").targetAmount(new BigDecimal("5000"))
                .targetDate(LocalDate.now().plusYears(1)).startDate(LocalDate.now()).user(testUser).build();
        when(goalRepository.findById(1L)).thenReturn(Optional.of(goal));
        when(goalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.findByUserIdAndDateOnOrAfter(eq(1L), any())).thenReturn(List.of());

        GoalUpdateRequest update = GoalUpdateRequest.builder().targetAmount(new BigDecimal("6000")).build();
        GoalResponse result = goalService.updateGoal(1L, update);

        assertThat(result.getTargetAmount()).isEqualByComparingTo("6000");
    }

    @Test
    @DisplayName("Should delete goal successfully")
    void deleteGoal_Success() {
        Goal goal = Goal.builder().id(1L).user(testUser).build();
        when(goalRepository.findById(1L)).thenReturn(Optional.of(goal));

        Map<String, String> result = goalService.deleteGoal(1L);

        assertThat(result.get("message")).isEqualTo("Goal deleted successfully");
        verify(goalRepository).delete(goal);
    }

    @Test
    @DisplayName("Should throw 403 when deleting other user's goal")
    void deleteGoal_Forbidden() {
        Goal goal = Goal.builder().id(2L).user(otherUser).build();
        when(goalRepository.findById(2L)).thenReturn(Optional.of(goal));

        assertThatThrownBy(() -> goalService.deleteGoal(2L))
                .isInstanceOf(ForbiddenException.class);
    }

    private void mockSecurityContext(String username) {
        SecurityContext context = mock(SecurityContext.class);
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getName()).thenReturn(username);
        lenient().when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);
    }
}
